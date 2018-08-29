package org.openmrs.module.aihdconfigs.tasks;

import org.apache.commons.io.FileUtils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.openmrs.util.OpenmrsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class CopyFilesToCorrectDirectoryTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(CopyFilesToCorrectDirectoryTask.class);

    @Override
    public void execute() {
        if (!isExecuting) {
            if (log.isDebugEnabled()) {
                log.debug("Copying files from a foreign folder on the system file to OpenMRS accessible folder");
            }

            startExecuting();
            try {
                //do all the work here
                copyFiles();
            } catch (Exception e) {
                log.error("Error while copying files to the respective directory", e);
            } finally {
                stopExecuting();
            }
        }
    }

    private void copyFiles() throws IOException {
        File destination = new File(OpenmrsUtil.getApplicationDataDirectory() + "/data_files");
        File source = new File("/var/www/html/patient_data");

        if (!destination.exists()) {
            FileUtils.forceMkdir(destination);
        }
        if(source.exists() && source.isDirectory()) {
            File [] content = source.listFiles();
            if(content != null && content.length > 0) {
                for (File file: content) {
                    FileUtils.moveFileToDirectory(file, destination, false);

                }
            }

        }
    }
}