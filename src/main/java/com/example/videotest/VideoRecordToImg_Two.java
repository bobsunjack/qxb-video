package com.example.videotest;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class VideoRecordToImg_Two extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(VideoRecordToImg_Two.class);
    public static BlockingQueue pics = new ArrayBlockingQueue(10000);
    public static int begin = 0;

    public static void main(String[] args) throws Exception {
        String inputFile = "rtsp://admin:hik12345@192.168.0.122:554/h264/ch1/main/av_stream";
        // Decodes-encodes
        String outputFile = "e://recorde.mp4";
        frameRecord(inputFile, outputFile, 1);
    }

    @Override
    public void run() {
        super.run();
        if (begin == 0) {
            begin++;
            String inputFile = "rtsp://admin:hik12345@192.168.0.122:554/h264/ch1/main/av_stream";
            // Decodes-encodes
            String outputFile = "e://recorde.mp4";
            try {
                frameRecord(inputFile, outputFile, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 按帧录制视频
     *
     * @param inputFile-该地址可以是网络直播/录播地址，也可以是远程/本地文件路径
     * @param outputFile                              -该地址只能是文件地址，如果使用该方法推送流媒体服务器会报错，原因是没有设置编码格式
     * @throws FrameRecorder.Exception
     */
    public static void frameRecord(String inputFile, String outputFile, int audioChannel)
            throws Exception, FrameRecorder.Exception {

        boolean isStart = true;//该变量建议设置为全局控制变量，用于控制录制结束
        // 获取视频源
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile);
        grabber.getFrameRate();
        grabber.setOption("rtsp_transport", "tcp");
       /* grabber.setFrameRate(110);
        //grabber.setVideoBitrate(3000000);
        grabber.setVideoBitrate(300);*/
        // 流媒体输出地址，分辨率（长，高），是否录制音频（0:不录制/1:录制）
        //  FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, 1280, 720, audioChannel);
        // FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, 110,100, audioChannel);
        // 开始取视频源
        recordByFrame(grabber, isStart);
    }

    private static void recordByFrame(FFmpegFrameGrabber grabber, Boolean status)
            throws Exception, FrameRecorder.Exception {
        try {//建议在线程中使用该方法
            grabber.start();
            Frame frame = null;
            int count = 0;
            while (status && (frame = grabber.grabFrame()) != null) {

                if ((count % 5 == 0) || (count % 9 == 0)|| (count % 11 == 0)) {

                    if (count > 85)
                        count = 1;
                    Java2DFrameConverter converter = new Java2DFrameConverter();
                    BufferedImage buf = converter.convert(frame);
                    if (buf != null) {
                        String source = BufferedImageToBase64(buf);
                        MyWebSocket.sendInfo(source);
                        // pics.put(source);
                        // System.out.println(source);

                    }
                    count++;
                } else {
                  //  System.out.println(count);
                    count++;
                    continue;
                }

            }

            grabber.stop();
        } catch (Exception e) {
            logger.error("异常", e);
        } finally {
            if (grabber != null) {
                grabber.stop();
            }
            begin = 0;
            new VideoRecordToImg_Two().start();
        }
    }


    public static String BufferedImageToBase64(BufferedImage image) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(outputStream.toByteArray());
    }
}
