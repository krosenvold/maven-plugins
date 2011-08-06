package org.apache.maven.plugins.site;

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


import org.apache.maven.plugin.MojoExecutionException;

/**
 * Deploys the generated site to a staging or mock directory to the site URL
 * specified in the <code>&lt;distributionManagement&gt;</code> section of the
 * POM. It supports <code>scp</code> and <code>file</code> protocols for
 * deployment.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal stage-deploy
 * @requiresDependencyResolution test
 */
public class SiteStageDeployMojo
    extends AbstractDeployMojo
{
    /**
     * The staged site will be deployed to this URL.
     *
     * If you don't specify this, the default-value will be
     * "${project.distributionManagement.site.url}/staging", where "project" is
     * either the current project or, in a reactor build, the top level project
     * in the reactor.
     * <p>
     * Note that even if you specify this plugin parameter you still need to indicate
     * ${project.distributionManagement.site.url} at least in your top level project
     * in order for relative links between modules to be resolved correctly.
     * </p>
     *
     * @parameter expression="${stagingSiteURL}"
     * @see <a href="http://maven.apache.org/maven-model/maven.html#class_site">MavenModel#class_site</a>
     */
    private String stagingSiteURL;

    /**
     * The identifier of the repository where the staging site will be deployed. This id will be used to lookup a
     * corresponding <code>&lt;server&gt;</code> entry from the <code>settings.xml</code>. If a matching
     * <code>&lt;server&gt;</code> entry is found, its configured credentials will be used for authentication.
     *
     * If this is not specified, then the corresponding value of <code>distributionManagement.site.id</code>
     * will be taken as default, unless this is not defined either then the String
     * <code>"stagingSite"</code> is used. (<strong>Note</strong>:
     * until v. 2.3 and 3.0-beta-3 the String <code>"stagingSite"</code> is always used.)
     *
     * @parameter expression="${stagingRepositoryId}"
     *
     * @since 2.0.1
     */
    private String stagingRepositoryId;

    @Override
    protected String getDeployRepositoryID()
        throws MojoExecutionException
    {
        stagingRepositoryId =  stagingRepoId ( stagingRepositoryId );

        getLog().info( "Using this server ID for stage deploy: " + stagingRepositoryId );

        return stagingRepositoryId;
    }

    @Override
    protected String getDeployRepositoryURL()
        throws MojoExecutionException
    {
        String stagingURL = determineStagingSiteURL( stagingSiteURL );

        getLog().info( "Using this base URL for stage deploy: " + stagingURL );

        return stagingURL;
    }

    /**
     * Find the URL where staging will take place.
     *
     * @param usersStagingSiteURL The staging site URL as suggested by the user's configuration
     * 
     * @return the site URL for staging
     */
    private String determineStagingSiteURL( final String usersStagingSiteURL )
        throws MojoExecutionException
    {
        String topLevelURL = null;

        if ( usersStagingSiteURL != null )
        {
            // the user has specified a stagingSiteURL - use it
            getLog().debug( "stagingSiteURL specified by the user: " + usersStagingSiteURL );
            topLevelURL = usersStagingSiteURL;
        }
        else
        {
            // The user didn't specify a URL, use the top level site distribution URL and add "[/]staging/" to it
            topLevelURL = appendSlash( getRootSite( project ).getUrl() )
                + DEFAULT_STAGING_DIRECTORY;
            getLog().debug( "stagingSiteURL NOT specified, using the top level project: " + topLevelURL );
        }

        // Return either
        //   usersURL
        // or
        //   topLevelProjectURL + "staging"
        return topLevelURL;
    }

    private String stagingRepoId( final String stagingRepoId )
    {
        if ( stagingRepoId != null )
        {
            return stagingRepoId;
        }

        try
        {
            return getSite( project ).getId();
        }
        catch ( MojoExecutionException ex )
        {
            return "stagingSite";
        }
    }
}
