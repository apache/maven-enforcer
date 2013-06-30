Deploying web site
-------------------
You can use the deploySite(.sh|.bat) script
Without any profile, the site will be deployed to http://maven.apache.org/enforcer-archives/enforcer-${project.version}
sh ./deploySite.sh -Preporting

To deploy main version http://maven.apache.org/enforcer, use
sh ./deploySite.sh -Preporting -Psite-release

Note you can add arguments to the script to pass your svn credentials:
-Dusername=
-Dpassword=

Workflow for site when releasing
--------------------------------
Once release staged, you can publish a staged site.
cd target/checkout
sh ./deploySite.sh -Preporting
content will be in http://maven.apache.org/enforcer-archives/enforcer-${project.version}

Once vote passed, redeploy main site:
cd target/checkout (or use the version tag)
sh ./deploySite.sh -Preporting -Psite-release

