package org.wso2.extension.siddhi.execution.ip;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.annotation.Parameter;
import org.wso2.siddhi.annotation.util.DataType;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.event.ComplexEvent;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventCloner;
import org.wso2.siddhi.core.event.stream.populater.ComplexEventPopulater;
import org.wso2.siddhi.core.executor.ConstantExpressionExecutor;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.query.processor.Processor;
import org.wso2.siddhi.core.query.processor.stream.StreamProcessor;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.exception.SiddhiAppValidationException;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 *
 *
 * **/
@Extension(
        name = "imageProcessor",
        namespace = "anusha",
        description = "imageProcessor will process a given image and identify the areas which matches the given color" +
                " intensities and return the ",
        parameters = {
                @Parameter(
                        name = "lower.color",
                        description = "lower bound of the Hue value which needed to be recognized",
                        type = DataType.STRING
                ),
                @Parameter(
                        name = "upper.color",
                        description = "upper bound of the Hue value which needed to be recognized",
                        type = DataType.STRING
                ),
                @Parameter(
                        name = "images.url",
                        description = "location of the images",
                        type = DataType.STRING,
                        optional = true,
                        defaultValue = "CARBON_HOME/resources/image-processing"
                )
        },
        examples = {
                @Example(
                        syntax = "from",
                        description = " "
                )
        }

)
public class ImageProcessor extends StreamProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ImageProcessor.class);
    private static final String CARBON_HOME = System.getProperty("carbon.home");
    private Scalar lowerColor;
    private Scalar upperColor;
    private String url;
    private Mat inputFrame = null;
    private Mat imageHSV = null;

    static {
        try {
//            System.load(CARBON_HOME + "/resources/image-processing/lib/libopencv_java342.dylib");
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            LOG.info("Loaded ************************** ");
            LOG.info("PATH::::: " + System.getProperty("java.library.path"));
        } catch (UnsatisfiedLinkError e) {
            LOG.error("Load Error ************************** ");

        } catch (Throwable t) {
            LOG.error("Load Error Unknown************************** ", t);

        }
    }


    protected List<Attribute> init(AbstractDefinition abstractDefimvnition, ExpressionExecutor[] expressionExecutors,
                                   ConfigReader configReader, SiddhiAppContext siddhiAppContext) {
        List<Attribute> outputAttributes = new ArrayList<>();
        LOG.info("PATH:::::init: " + System.getProperty("java.library.path"));

//        System.load(CARBON_HOME + "/resources/image-processing/lib/libopencv_java342.dylib");
        outputAttributes.add(new Attribute("TimeStamp", Attribute.Type.STRING));
        outputAttributes.add(new Attribute("detectedPixels", Attribute.Type.INT));
        outputAttributes.add(new Attribute("totalPixels", Attribute.Type.INT));

        int inputExecutorLength = attributeExpressionExecutors.length;
        if (inputExecutorLength >= 2) {
            if (attributeExpressionExecutors[0].getReturnType().equals(Attribute.Type.STRING)) {

                lowerColor = validateAndbuildScaler(((ConstantExpressionExecutor) attributeExpressionExecutors[0])
                        .getValue().toString());
            } else {
                throw new SiddhiAppValidationException("Lower bound of the Hue value which needed to be recognized " +
                        "should be defined as a string in following format 'xx,xx,xx' where xx is a double value " +
                        "between 0 - " +
                        "255, but found " + ((ConstantExpressionExecutor) attributeExpressionExecutors[0])
                        .getValue().toString());
            }
            if (attributeExpressionExecutors[1].getReturnType().equals(Attribute.Type.STRING)) {
                upperColor = validateAndbuildScaler(((ConstantExpressionExecutor) attributeExpressionExecutors[1])
                        .getValue().toString());
            } else {
                throw new SiddhiAppValidationException("Lower bound of the Hue value which needed to be recognized " +
                        "should be defined as a string in following format 'xx,xx,xx' where xx is a double value " +
                        "between 0 - " +
                        "255, but found " + ((ConstantExpressionExecutor) attributeExpressionExecutors[1])
                        .getValue().toString());
            }
        } else {
            throw new SiddhiAppValidationException("Lower bound and Upper bound values are mandatory parameters");
        }
        if (inputExecutorLength == 3) {
            if (attributeExpressionExecutors[2].getReturnType().equals(Attribute.Type.STRING)) {
                url = ((ConstantExpressionExecutor) attributeExpressionExecutors[2])
                        .getValue().toString();
            } else {
                throw new SiddhiAppValidationException("Image uri should be a String which contains the physical " +
                        "location of the images on the server");
            }

        } else {
            url = CARBON_HOME + "/resources/image-processing";
        }

        return outputAttributes;
    }

    protected void process(ComplexEventChunk<StreamEvent> complexEventChunk, Processor processor,
                           StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {
//        System.load(CARBON_HOME + "/resources/image-processing/lib/libopencv_java342.dylib");
        LOG.info("PATH:::::process: " + System.getProperty("java.library.path"));

        File[] imageFiles = new File(url).listFiles();
        while (complexEventChunk.hasNext()) {
            ComplexEvent complexEvent = complexEventChunk.next();
            if (Objects.nonNull(imageFiles)) {
                for (File imageFile : imageFiles) {
                    if (imageFile.isFile()) {
                        try {
                            inputFrame = Imgcodecs.imread(imageFile.getAbsolutePath(),
                                    Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
                            Object[] outputObject = processImage(inputFrame);
                            complexEventPopulater.populateComplexEvent(complexEvent, outputObject);
                        } catch (Throwable t) {
                            LOG.error("Pannaaaaaa", t);
                        }
                    }
                }
            }
        }
        nextProcessor.process(complexEventChunk);
    }

    public Object[] processImage(Mat image) {
        Object[] outputObjects = new Object[3];
        imageHSV = image;
        List<Mat> channels = new ArrayList<>();
        Imgproc.cvtColor(image, imageHSV, Imgproc.COLOR_BGR2HSV);
        Core.split(imageHSV, channels);
        int totalPixels = imageHSV.cols() * imageHSV.rows();
        Mat mask = image.clone();
        Core.inRange(imageHSV, lowerColor, upperColor, mask);
        outputObjects[0] = LocalDateTime.now().toString();
        outputObjects[1] = Core.countNonZero(mask);
        outputObjects[2] = totalPixels;
        return outputObjects;

    }

//    private void displayImage(Mat image, String name) throws IOException {
//        BufferedImage bufImage = null;
//        MatOfByte matOfByte = new MatOfByte();
//        Imgcodecs.imencode(".jpg", image, matOfByte);
//        byte[] byteArray = matOfByte.toArray();
//        InputStream in = new ByteArrayInputStream(byteArray);
//        bufImage = ImageIO.read(in);
//
//        JFrame frame = new JFrame(name);
//        frame.getContentPane().setLayout(new FlowLayout());
//        frame.getContentPane().add(new JLabel(new ImageIcon(bufImage)));
//        frame.pack();
//        frame.setVisible(true);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//    }

    private Scalar validateAndbuildScaler(String value) {
        String[] hueValues = value.split(",");
        try {
            return new Scalar(Double.parseDouble(hueValues[0]), Double.parseDouble(hueValues[1]), Double.parseDouble
                    (hueValues[2]));
        } catch (NumberFormatException e) {
            throw new SiddhiAppValidationException("Provided Hue bound values should be in " +
                    "following format 'xx,xx,xx' where  xx should be a double vvalue between 0 - 255 but" +
                    " found :'" + value + "'");
        }
    }

    public void start() {

    }

    public void stop() {

    }

    public Map<String, Object> currentState() {
        return null;
    }

    public void restoreState(Map<String, Object> map) {

    }

}
