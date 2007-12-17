package org.apache.maven.plugins.enforcer;

import java.io.File;

public class RequireFilesDontExist
    extends AbstractRequireFiles
{

    boolean checkFile( File file )
    {
        return !file.exists();
    }

    String getErrorMsg()
    {
        return "Some files should not exist:\n";
    }

}
