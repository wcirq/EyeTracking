package com.wcy.eyetracking.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Trace;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Vector;

public class EyeModel {
    static {
        System.loadLibrary("tensorflow_inference");
    }

    //PATH TO OUR MODEL FILE AND NAMES OF THE INPUT AND OUTPUT NODES
    private String MODEL_PATH = "file:///android_asset/frozen_inference_graph_v6.pb";
    private String MODEL_LABEL_PATH = "coco_labels_list.txt";
    private String INPUT_NAME = "image_tensor:0";
    private String OUTPUT_NAMES[] = new String[] {"detection_boxes", "detection_scores", "detection_classes", "num_detections"};
    private TensorFlowInferenceInterface tf;
    private static final int MAX_RESULTS = 100;
    private float []outputLocations = new float[MAX_RESULTS * 4];
    private float []outputScores = new float[MAX_RESULTS];
    private float []outputClasses = new float[MAX_RESULTS];
    private float []outputNumDetections = new float[1];
    public int inputSize=300;
    private Vector<String> labels = new Vector<String>();

    private byte[] floatValues = new byte[inputSize * inputSize * 3];;
    private int[] intValues = new int[inputSize * inputSize];;
    private int[] INPUT_SIZE = {inputSize,inputSize,3};
    String verify_titles[] = {"打字机", "调色板", "跑步机", "毛线", "老虎", "安全帽", "沙包", "盘子", "本子", "药片", "双面胶", "龙舟", "红酒", "拖把", "卷尺", "海苔", "红豆", "黑板", "热水袋", "烛台", "钟表", "路灯", "沙拉", "海报", "公交卡", "樱桃", "创可贴", "牌坊", "苍蝇拍", "高压锅", "电线", "网球拍", "海鸥", "风铃", "订书机", "冰箱", "话梅", "排风机", "锅铲", "绿豆", "航母", "电子秤", "红枣", "金字塔", "鞭炮", "菠萝", "开瓶器", "电饭煲", "仪表盘", "棉棒", "篮球", "狮子", "蚂蚁", "蜡烛", "茶盅", "印章", "茶几", "啤酒", "档案袋", "挂钟", "刺绣", "铃铛", "护腕", "手掌印", "锦旗", "文具盒", "辣椒酱", "耳塞", "中国结", "蜥蜴", "剪纸", "漏斗", "锣", "蒸笼", "珊瑚", "雨靴", "薯条", "蜜蜂", "日历", "口哨"};

    public EyeModel(AssetManager assetManager){
        tf = new TensorFlowInferenceInterface(assetManager,MODEL_PATH);
        InputStream labelsInput = null;
        try {
            labelsInput = assetManager.open(MODEL_LABEL_PATH);
            BufferedReader br = null;
            br = new BufferedReader(new InputStreamReader(labelsInput));
            String line;
            while ((line = br.readLine()) != null) {
                labels.add(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object[] argmax(float[] array){


        int best = -1;
        float best_confidence = 0.0f;

        for(int i = 0;i < array.length;i++){

            float value = array[i];

            if (value > best_confidence){

                best_confidence = value;
                best = i;
            }
        }

        return new Object[]{best,best_confidence};
    }

    public static Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation,
            final boolean maintainAspectRatio) {
        final Matrix matrix = new Matrix();

        if (applyRotation != 0) {
            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

        // Account for the already applied rotation, if any, and then determine how
        // much scaling is needed for each axis.
        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        final int inWidth = transpose ? srcHeight : srcWidth;
        final int inHeight = transpose ? srcWidth : srcHeight;

        // Apply scaling if necessary.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;

            if (maintainAspectRatio) {
                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                // Scale exactly to fill dst from src.
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }

    public ArrayList<Recognition> predict(final Bitmap bitmap) {
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < intValues.length; ++i) {
            floatValues[i * 3 + 2] = (byte) (intValues[i] & 0xFF);
            floatValues[i * 3 + 1] = (byte) ((intValues[i] >> 8) & 0xFF);
            floatValues[i * 3 + 0] = (byte) ((intValues[i] >> 16) & 0xFF);
        }

        tf.feed(INPUT_NAME, floatValues, 1, INPUT_SIZE[0], INPUT_SIZE[1], INPUT_SIZE[2]);
        tf.run(OUTPUT_NAMES);
        tf.fetch(OUTPUT_NAMES[0], outputLocations);
        tf.fetch(OUTPUT_NAMES[1], outputScores);
        tf.fetch(OUTPUT_NAMES[2], outputClasses);
        tf.fetch(OUTPUT_NAMES[3], outputNumDetections);

        final PriorityQueue<Recognition> pq =
                new PriorityQueue<Recognition>(
                        1,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(final Recognition lhs, final Recognition rhs) {
                                // Intentionally reversed to put high confidence at the head of the queue.
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });

        for (int i = 0; i < outputScores.length; ++i) {
            final RectF detection =
                    new RectF(
                            outputLocations[4 * i + 1] * inputSize,
                            outputLocations[4 * i] * inputSize,
                            outputLocations[4 * i + 3] * inputSize,
                            outputLocations[4 * i + 2] * inputSize);
            pq.add(
                    new Recognition("" + i, labels.get((int) outputClasses[i]), outputScores[i], detection));
        }

        final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
        for (int i = 0; i < Math.min(pq.size(), MAX_RESULTS); ++i) {
            recognitions.add(pq.poll());
        }
        return recognitions;
    }

    public class Recognition {
        /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        private final String id;

        /**
         * Display name for the recognition.
         */
        private final String title;

        /**
         * A sortable score for how good the recognition is relative to others. Higher should be better.
         */
        private final Float confidence;

        /** Optional location within the source image for the location of the recognized object. */
        private RectF location;

        public Recognition(
                final String id, final String title, final Float confidence, final RectF location) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            if (location != null) {
                resultString += location + " ";
            }

            return resultString.trim();
        }
    }
}

