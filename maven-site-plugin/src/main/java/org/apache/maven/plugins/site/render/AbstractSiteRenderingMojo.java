package org.apache.maven.plugins.site.render;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.Menu;
import org.apache.maven.doxia.site.decoration.MenuItem;
import org.apache.maven.doxia.site.decoration.inheritance.DecorationModelInheritanceAssembler;
import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.RenderingContext;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.tools.SiteToolException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.site.AbstractSiteMojo;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.exec.MavenReportExecution;
import org.apache.maven.reporting.exec.MavenReportExecutor;
import org.apache.maven.reporting.exec.MavenReportExecutorRequest;
import org.apache.maven.reporting.exec.ReportPlugin;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * Base class for site rendering mojos.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractSiteRenderingMojo
    extends AbstractSiteMojo implements Contextualizable
{
    /**
     * Module type exclusion mappings
     * ex: <code>fml  -> **&#47;*-m1.fml</code>  (excludes fml files ending in '-m1.fml' recursively)
     * <p/>
     * The configuration looks like this:
     * <pre>
     *   &lt;moduleExcludes&gt;
     *     &lt;moduleType&gt;filename1.ext,**&#47;*sample.ext&lt;/moduleType&gt;
     *     &lt;!-- moduleType can be one of 'apt', 'fml' or 'xdoc'. --&gt;
     *     &lt;!-- The value is a comma separated list of           --&gt;
     *     &lt;!-- filenames or fileset patterns.                   --&gt;
     *     &lt;!-- Here's an example:                               --&gt;
     *     &lt;xdoc&gt;changes.xml,navigation.xml&lt;/xdoc&gt;
     *   &lt;/moduleExcludes&gt;
     * </pre>
     */
    @Parameter
    private Map<String, String> moduleExcludes;

    /**
     * The component for assembling inheritance.
     */
    @Component
    private DecorationModelInheritanceAssembler assembler;

    /**
     * Remote repositories used for the project.
     *
     * @todo this is used for site descriptor resolution - it should relate to the actual project but for some reason they are not always filled in
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true )
    private List<ArtifactRepository> repositories;

    /**
     * Directory containing the template page.
     *
     * @deprecated use templateFile or skinning instead
     */
    @Parameter( property = "templateDirectory", defaultValue = "src/site" )
    private File templateDirectory;

    /**
     * Default template page.
     *
     * @deprecated use templateFile or skinning instead
     */
    @Parameter( property = "template" )
    private String template;

    /**
     * The location of a Velocity template file to use. When used, skins and the default templates, CSS and images
     * are disabled. It is highly recommended that you package this as a skin instead.
     *
     * @since 2.0-beta-5
     */
    @Parameter( property = "templateFile" )
    private File templateFile;

    /**
     * Additional template properties for rendering the site. See
     * <a href="/doxia/doxia-sitetools/doxia-site-renderer/">Doxia Site Renderer</a>.
     */
    @Parameter
    private Map<String, Object> attributes;

    /**
     * Site renderer.
     */
    @Component
    protected Renderer siteRenderer;

    /**
     * Reports (Maven 2).
     */
    @Parameter( defaultValue = "${reports}", required = true, readonly = true )
    protected List<MavenReport> reports;

    /**
     * Alternative directory for xdoc source, useful for m1 to m2 migration
     *
     * @deprecated use the standard m2 directory layout
     */
    @Parameter( defaultValue = "${basedir}/xdocs" )
    private File xdocDirectory;

    /**
     * Directory containing generated documentation.
     * This is used to pick up other source docs that might have been generated at build time.
     *
     * @todo should we deprecate in favour of reports?
     */
    @Parameter( alias = "workingDirectory", defaultValue = "${project.build.directory}/generated-site" )
    protected File generatedSiteDirectory;

    /**
     * The current Maven session.
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    protected MavenSession mavenSession;

    /**
     * <p>Configuration section <b>used internally</b> by Maven 3.</p>
     * <p>More details available here:
     * <a href="http://maven.apache.org/plugins/maven-site-plugin/maven-3.html#Configuration_formats" target="_blank">
     * http://maven.apache.org/plugins/maven-site-plugin/maven-3.html#Configuration_formats</a>
     * </p>
     * <p><b>Note:</b> using this field is not mandatory with Maven 3 as Maven core injects usual
     * <code>&lt;reporting&gt;</code> section into this field.</p>
     *
     * @since 3.0-beta-1 (and read-only since 3.3)
     */
    @Parameter( readonly = true )
    private ReportPlugin[] reportPlugins;

    private PlexusContainer container;

    /**
     * Make links in the site descriptor relative to the project URL.
     * By default, any absolute links that appear in the site descriptor,
     * e.g. banner hrefs, breadcrumbs, menu links, etc., will be made relative to project.url.
     * <p/>
     * Links will not be changed if this is set to false, or if the project has no URL defined.
     *
     * @since 2.3
     */
    @Parameter( property = "relativizeDecorationLinks", defaultValue = "true" )
    private boolean relativizeDecorationLinks;

    /**
     * Whether to generate the summary page for project reports: project-info.html.
     *
     * @since 2.3
     */
    @Parameter( property = "generateProjectInfo", defaultValue = "true" )
    private boolean generateProjectInfo;

    /** {@inheritDoc} */
    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    protected List<MavenReportExecution> getReports()
        throws MojoExecutionException
    {
        List<MavenReportExecution> allReports;

        if ( isMaven3OrMore() )
        {
            // Maven 3
            MavenReportExecutorRequest mavenReportExecutorRequest = new MavenReportExecutorRequest();
            mavenReportExecutorRequest.setLocalRepository( localRepository );
            mavenReportExecutorRequest.setMavenSession( mavenSession );
            mavenReportExecutorRequest.setProject( project );
            mavenReportExecutorRequest.setReportPlugins( reportPlugins );

            MavenReportExecutor mavenReportExecutor;
            try
            {
                mavenReportExecutor = (MavenReportExecutor) container.lookup( MavenReportExecutor.class.getName() );
            }
            catch ( ComponentLookupException e )
            {
                throw new MojoExecutionException( "could not get MavenReportExecutor component", e );
            }

            allReports = mavenReportExecutor.buildMavenReports( mavenReportExecutorRequest );
        }
        else
        {
            // Maven 2
            allReports = new ArrayList<MavenReportExecution>( reports.size() );
            for ( MavenReport report : reports )
            {
                allReports.add( new MavenReportExecution( report ) );
            }
        }

        // filter out reports that can't be generated
        List<MavenReportExecution> reportExecutions = new ArrayList<MavenReportExecution>( allReports.size() );
        for ( MavenReportExecution exec : allReports )
        {
            if ( exec.canGenerateReport() )
            {
                reportExecutions.add( exec );
            }
        }

        return reportExecutions;
    }

    protected SiteRenderingContext createSiteRenderingContext( Locale locale )
        throws MojoExecutionException, IOException, MojoFailureException
    {
        if ( attributes == null )
        {
            attributes = new HashMap<String, Object>();
        }

        if ( attributes.get( "project" ) == null )
        {
            attributes.put( "project", project );
        }

        if ( attributes.get( "inputEncoding" ) == null )
        {
            attributes.put( "inputEncoding", getInputEncoding() );
        }

        if ( attributes.get( "outputEncoding" ) == null )
        {
            attributes.put( "outputEncoding", getOutputEncoding() );
        }

        // Put any of the properties in directly into the Velocity context
        for ( Map.Entry<Object, Object> entry : project.getProperties().entrySet() )
        {
            attributes.put( (String) entry.getKey(), entry.getValue() );
        }

        DecorationModel decorationModel;
        try
        {
            decorationModel = siteTool.getDecorationModel( project, reactorProjects, localRepository, repositories,
                                                           siteTool.getRelativePath( siteDirectory.getAbsolutePath(),
                                                           project.getBasedir().getAbsolutePath() ),
                                                           locale );
        }
        catch ( SiteToolException e )
        {
            throw new MojoExecutionException( "SiteToolException: " + e.getMessage(), e );
        }

        if ( relativizeDecorationLinks )
        {
            final String url = project.getUrl();

            if ( url == null )
            {
                getLog().warn( "No project URL defined - decoration links will not be relativized!" );
            }
            else
            {
                // MSITE-658
                final String localeUrl =
                    locale.equals( Locale.getDefault() ) ? url : url + "/" + locale.getLanguage();
                getLog().info( "Relativizing decoration links with respect to project URL: " + localeUrl );
                assembler.resolvePaths( decorationModel, localeUrl );
            }
        }

        if ( template != null )
        {
            if ( templateFile != null )
            {
                getLog().warn( "'template' configuration is ignored when 'templateFile' is set" );
            }
            else
            {
                templateFile = new File( templateDirectory, template );
            }
        }

        File skinFile;
        try
        {
            Artifact skinArtifact =
                siteTool.getSkinArtifactFromRepository( localRepository, repositories, decorationModel );
            getLog().info( "Rendering site with " + skinArtifact.getId() + " skin." );

            skinFile = skinArtifact.getFile();
        }
        catch ( SiteToolException e )
        {
            throw new MojoExecutionException( "SiteToolException: " + e.getMessage(), e );
        }
        SiteRenderingContext context;
        if ( templateFile != null )
        {
            if ( !templateFile.exists() )
            {
                throw new MojoFailureException( "Template file '" + templateFile + "' does not exist" );
            }
            context = siteRenderer.createContextForTemplate( templateFile, skinFile, attributes, decorationModel,
                                                             project.getName(), locale );
        }
        else
        {
            context = siteRenderer.createContextForSkin( skinFile, attributes, decorationModel, project.getName(),
                                                         locale );
        }

        // Generate static site
        if ( !locale.getLanguage().equals( Locale.getDefault().getLanguage() ) )
        {
            context.addSiteDirectory( new File( siteDirectory, locale.getLanguage() ) );
            context.addModuleDirectory( new File( xdocDirectory, locale.getLanguage() ), "xdoc" );
            context.addModuleDirectory( new File( xdocDirectory, locale.getLanguage() ), "fml" );
        }
        else
        {
            context.addSiteDirectory( siteDirectory );
            context.addModuleDirectory( xdocDirectory, "xdoc" );
            context.addModuleDirectory( xdocDirectory, "fml" );
        }

        if ( moduleExcludes != null )
        {
            context.setModuleExcludes( moduleExcludes );
        }

        return context;
    }

    /**
     * Go through the list of reports and process each one like this:
     * <ul>
     * <li>Add the report to a map of reports keyed by filename having the report itself as value
     * <li>If the report is not yet in the map of documents, add it together with a suitable renderer
     * </ul>
     *
     * @param reports A List of MavenReports
     * @param documents A Map of documents, keyed by filename
     * @param locale the Locale the reports are processed for.
     * @return A map with all reports keyed by filename having the report itself as value.
     * The map will be used to populate a menu.
     */
    protected Map<String, MavenReport> locateReports( List<MavenReportExecution> reports,
                                                      Map<String, DocumentRenderer> documents, Locale locale )
    {
        // copy Collection to prevent ConcurrentModificationException
        List<MavenReportExecution> filtered = new ArrayList<MavenReportExecution>( reports );

        Map<String, MavenReport> reportsByOutputName = new LinkedHashMap<String, MavenReport>();
        for ( MavenReportExecution mavenReportExecution : filtered )
        {
            MavenReport report = mavenReportExecution.getMavenReport();

            String outputName = report.getOutputName() + ".html";

            // Always add the report to the menu, see MSITE-150
            reportsByOutputName.put( report.getOutputName(), report );

            if ( documents.containsKey( outputName ) )
            {
                String displayLanguage = locale.getDisplayLanguage( Locale.ENGLISH );

                String reportMojoInfo =
                    ( mavenReportExecution.getGoal() == null ) ? "" : ( " ("
                        + mavenReportExecution.getPlugin().getArtifactId() + ':'
                        + mavenReportExecution.getPlugin().getVersion() + ':' + mavenReportExecution.getGoal() + ')' );

                getLog().info( "Skipped \"" + report.getName( locale ) + "\" report" + reportMojoInfo + ", file \""
                                   + outputName + "\" already exists for the " + displayLanguage + " version." );

                reports.remove( mavenReportExecution );
            }
            else
            {
                RenderingContext renderingContext = new RenderingContext( siteDirectory, outputName );
                DocumentRenderer renderer = new ReportDocumentRenderer( mavenReportExecution, renderingContext, getLog() );
                documents.put( outputName, renderer );
            }
        }
        return reportsByOutputName;
    }

    /**
     * Go through the collection of reports and put each report into a list for the appropriate category. The list is
     * put into a map keyed by the name of the category.
     *
     * @param reports A Collection of MavenReports
     * @return A map keyed category having the report itself as value
     */
    protected Map<String, List<MavenReport>> categoriseReports( Collection<MavenReport> reports )
    {
        Map<String, List<MavenReport>> categories = new LinkedHashMap<String, List<MavenReport>>();
        for ( MavenReport report : reports )
        {
            List<MavenReport> categoryReports = categories.get( report.getCategoryName() );
            if ( categoryReports == null )
            {
                categoryReports = new ArrayList<MavenReport>();
                categories.put( report.getCategoryName(), categoryReports );
            }
            categoryReports.add( report );
        }
        return categories;
    }

    /**
     * Locate every document to be rendered for given locale:<ul>
     * <li>handwritten content,</li>
     * <li>reports,</li>
     * <li>"Project Information" and "Project Reports" category summaries.</li>
     * </ul>
     */
    protected Map<String, DocumentRenderer> locateDocuments( SiteRenderingContext context, List<MavenReportExecution> reports,
                                                             Locale locale )
        throws IOException, RendererException
    {
        Map<String, DocumentRenderer> documents = siteRenderer.locateDocumentFiles( context );

        Map<String, MavenReport> reportsByOutputName = locateReports( reports, documents, locale );

        // TODO: I want to get rid of categories eventually. There's no way to add your own in a fully i18n manner
        Map<String, List<MavenReport>> categories = categoriseReports( reportsByOutputName.values() );

        siteTool.populateReportsMenu( context.getDecoration(), locale, categories );
        populateReportItems( context.getDecoration(), locale, reportsByOutputName );

        if ( categories.containsKey( MavenReport.CATEGORY_PROJECT_INFORMATION ) && generateProjectInfo )
        {
            // add "Project Information" category summary document
            List<MavenReport> categoryReports = categories.get( MavenReport.CATEGORY_PROJECT_INFORMATION );

            RenderingContext renderingContext = new RenderingContext( siteDirectory, "project-info.html" );
            String title = i18n.getString( "site-plugin", locale, "report.information.title" );
            String desc1 = i18n.getString( "site-plugin", locale, "report.information.description1" );
            String desc2 = i18n.getString( "site-plugin", locale, "report.information.description2" );
            DocumentRenderer renderer = new CategorySummaryDocumentRenderer( renderingContext, title, desc1, desc2,
                                                                             i18n, categoryReports, getLog() );

            if ( !documents.containsKey( renderer.getOutputName() ) )
            {
                documents.put( renderer.getOutputName(), renderer );
            }
            else
            {
                getLog().info( "Category summary '" + renderer.getOutputName() + "' skipped; already exists" );
            }
        }

        if ( categories.containsKey( MavenReport.CATEGORY_PROJECT_REPORTS ) )
        {
            // add "Project Reports" category summary document
            List<MavenReport> categoryReports = categories.get( MavenReport.CATEGORY_PROJECT_REPORTS );
            RenderingContext renderingContext = new RenderingContext( siteDirectory, "project-reports.html" );
            String title = i18n.getString( "site-plugin", locale, "report.project.title" );
            String desc1 = i18n.getString( "site-plugin", locale, "report.project.description1" );
            String desc2 = i18n.getString( "site-plugin", locale, "report.project.description2" );
            DocumentRenderer renderer = new CategorySummaryDocumentRenderer( renderingContext, title, desc1, desc2,
                                                                             i18n, categoryReports, getLog() );

            if ( !documents.containsKey( renderer.getOutputName() ) )
            {
                documents.put( renderer.getOutputName(), renderer );
            }
            else
            {
                getLog().info( "Category summary '" + renderer.getOutputName() + "' skipped; already exists" );
            }
        }
        return documents;
    }

    protected void populateReportItems( DecorationModel decorationModel, Locale locale,
                                        Map<String, MavenReport> reportsByOutputName )
    {
        for ( Menu menu : decorationModel.getMenus() )
        {
            populateItemRefs( menu.getItems(), locale, reportsByOutputName );
        }
    }

    private void populateItemRefs( List<MenuItem> items, Locale locale, Map<String, MavenReport> reportsByOutputName )
    {
        for ( Iterator<MenuItem> i = items.iterator(); i.hasNext(); )
        {
            MenuItem item = i.next();

            if ( item.getRef() != null )
            {
                MavenReport report = reportsByOutputName.get( item.getRef() );

                if ( report != null )
                {
                    if ( item.getName() == null )
                    {
                        item.setName( report.getName( locale ) );
                    }

                    if ( item.getHref() == null || item.getHref().length() == 0 )
                    {
                        item.setHref( report.getOutputName() + ".html" );
                    }
                }
                else
                {
                    getLog().warn( "Unrecognised reference: '" + item.getRef() + "'" );
                    i.remove();
                }
            }

            populateItemRefs( item.getItems(), locale, reportsByOutputName );
        }
    }
}
