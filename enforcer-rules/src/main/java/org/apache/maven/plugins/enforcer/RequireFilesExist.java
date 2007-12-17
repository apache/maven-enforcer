package org.apache.maven.plugins.enforcer;

import java.io.File;

public class RequireFilesExist
    extends AbstractRequireFiles
{

    boolean checkFile( File file )
    {
        return file.exists();
    }

    String getErrorMsg()
    {
        return "Some required files are missing:\n";
    }

}
