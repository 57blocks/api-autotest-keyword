package api.autotest;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.robotframework.javalib.annotation.RobotKeywords;
import org.robotframework.javalib.library.AnnotationLibrary;

@RobotKeywords
public class BasicLibrary extends AnnotationLibrary {
    public static final String ROBOT_LIBRARY_SCOPE = "GLOBAL";
    private final static Logger LOGGER = Logger.getLogger(BasicLibrary.class);

    static List<String> keywordPatterns = new ArrayList<String>() {
        private static final long serialVersionUID = 1L;
        {
            add("api/autotest/keywords/**/*.class");
        }
    };


    public BasicLibrary() {
        super(keywordPatterns);
        LOGGER.info("User defined keywords loaded.");
    }

}