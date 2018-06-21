package org.openmrs.module.aihdconfigs.tasks;

import org.openmrs.module.aihdconfigs.utils.JSONParserUtil;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadJSONTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(ReadJSONTask.class);

    @Override
    public void execute() {
        if (!isExecuting) {
            if (log.isDebugEnabled()) {
                log.error("Starting JSON file reader task...");
            }

            startExecuting();
            try {
                JSONParserUtil.readJSOFile();
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Error reading JSON file :", e);
            } finally {
                stopExecuting();
            }
        }
    }
}
