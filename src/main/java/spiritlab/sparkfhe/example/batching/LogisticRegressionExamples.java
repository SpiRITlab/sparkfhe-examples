package spiritlab.sparkfhe.example.batching;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.mllib.linalg.*;
import org.apache.spark.mllib_fhe.linalg.*;
import org.apache.spark.spiritlab.sparkfhe.SparkFHEPlugin;
import org.apache.spark.sql.SparkSession;
import spiritlab.sparkfhe.api.Ciphertext;
import spiritlab.sparkfhe.api.DoubleVector;
import spiritlab.sparkfhe.api.SparkFHE;
import spiritlab.sparkfhe.example.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LogisticRegressionExamples {
    private static double[][] X;
    private static double[] y;
    private static int NROWS;
    private static int NCOLS;
    private static final int SLICES = 20;

    private static void setXy(String filePath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            NROWS = lines.size();
            NCOLS = lines.get(0).split(",").length;
            X = new double[NROWS][NCOLS];
            y = new double[NROWS];

            int i = 0;
            for (String line: lines) {
                String[] arr = line.split(",");
                X[i][0] = 1;
                X[i][1] = Double.parseDouble(arr[0]);
                X[i][2] = Double.parseDouble(arr[1]);
                y[i] = Double.parseDouble(arr[2]);
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double sigmoid(double x) {
        return 1.0/(1 + Math.exp(-x));
    }

    private static Split trainTestSplit(double trainSize) {
        List<Integer> indices = new ArrayList<>(NROWS);
        for (int i = 0; i < NROWS; i++) indices.add(i);
        Collections.shuffle(indices);

        int trainLength = (int) (NROWS * trainSize);
        double [][] trainX = new double[trainLength][NCOLS];
        double [][] testX = new double[NROWS-trainLength][NCOLS];
        double [] trainY = new double[trainLength];
        double [] testY = new double[NROWS-trainLength];

        for (int i = 0; i < trainLength; i++) {
            int index = indices.get(i);
            for (int j = 0; j < NCOLS; j++) {
                trainX[i][j] = X[index][j];
            }
            trainY[i] = y[index];
        }

        for (int i = trainLength; i < NROWS; i++) {
            int index = indices.get(i);
            for (int j = 0; j < NCOLS; j++) {
                testX[i-trainLength][j] = X[index][j];
            }
            testY[i-trainLength] = y[index];
        }

        Split split = new Split();
        split.trainX = trainX;
        split.testX = testX;
        split.trainY = trainY;
        split.testY = testY;
        split.testLength = NROWS - trainLength;

        return split;
    }

    static class Split {
        double [][] trainX;
        double [][] testX;
        double [] trainY;
        double [] testY;
        int testLength;
    }

    private static DenseVector sigmoid(DenseVector vector) {
        double [] values = vector.values();
        for (int i = 0; i < values.length; i++) {
            values[i] = sigmoid(values[i]);
        }
        return vector;
    }

    private static DenseVector sigmoidPoly(DenseVector vector, int degree) {
        double [] values = vector.values();
        if (degree == 3) {
            g3(values);
        } else if (degree == 5) {
            g5(values);
        } else if (degree == 7) {
            g7(values);
        } else {
            return sigmoid(vector);
        }

        return vector;
    }

    private static void g3(double [] values) {
        for (int i = 0; i < values.length; i++) {
            double x = values[i];
            values[i] = 0.5 - 1.20096 * (x/8) + 0.81562 * Math.pow(x/8, 3);
        }
    }

    private static void g5(double [] values) {
        for (int i = 0; i < values.length; i++) {
            double x = values[i];
            values[i] = 0.5 - 1.53048 * (x/8) + 2.3533056 * Math.pow(x/8, 3) - 1.3511295 * Math.pow(x/8, 5);
        }
    }

    private static void g7(double [] values) {
        for (int i = 0; i < values.length; i++) {
            double x = values[i];
            values[i] = 0.5 - 1.73496 * (x/8) + 4.19407 * Math.pow(x/8, 3) - 5.43402 * Math.pow(x/8, 5) +
                    2.50739 * Math.pow(x/8, 7);
        }
    }

    private static CtxtDenseVector sigmoid(CtxtDenseVector vector) {
        String [] values = vector.values();
        for (int i = 0; i < values.length; i++) {
            values[i] = SparkFHE.getInstance().fhe_add(
                    SparkFHE.getInstance().encrypt(SparkFHE.getInstance().encode("0.5")).toString(),
                    SparkFHE.getInstance().fhe_multiply(
                            SparkFHE.getInstance().encrypt(SparkFHE.getInstance().encode("0.197")).toString(),
                            SparkFHE.getInstance().fhe_multiply(values[i], values[i])
                    )
            );
        }
        return vector;
    }

    private static void minMaxScaler(double[][] arr) {
        for (int j = 1; j < arr[0].length; j++) {
            double min = arr[0][j];
            double max = arr[0][j];
            for (int i = 0; i < arr.length; i++) {
                if (arr[i][j] < min) min = arr[i][j];
                if (arr[i][j] > max) max = arr[i][j];
            }
            for (int i = 0; i < arr.length; i++) {
                arr[i][j] = (arr[i][j] - min)/(max - min);
            }
        }
    }

    private static List<Matrix> getList(double[][] arr, int slices) {
        int numRows = arr.length;
        int chunk = (numRows % slices == 0)? numRows/slices: numRows/slices+1;
        List<Matrix> result = new ArrayList<>();

        for (int i = 0; i < numRows; i+=chunk) {
            double [][] subArray = Arrays.copyOfRange(arr, i, Math.min(numRows, i+chunk));
            double [] values = new double[subArray.length * subArray[0].length];
            for (int j = 0; j < subArray[0].length; j++) {
                for (int k = 0; k < subArray.length; k++) {
                    values[j * subArray.length + k] = subArray[k][j];
                }
            }
            result.add(Matrices.dense(subArray.length, subArray[0].length, values));
        }

        return result;
    }

    private static List<CtxtMatrix> getListCtxt(double[][] arr, int slices) {
        int numRows = arr.length;
        int chunk = (numRows % slices == 0)? numRows/slices: numRows/slices+1;
        List<CtxtMatrix> result = new ArrayList<>();

        for (int i = 0; i < numRows; i+=chunk) {
            double[][] subArray = Arrays.copyOfRange(arr, i, Math.min(numRows, i + chunk));
            String[] values = new String[subArray.length * subArray[0].length];
            for (int j = 0; j < subArray[0].length; j++) {
                for (int k = 0; k < subArray.length; k++) {
                    values[j * subArray.length + k] = SparkFHE.getInstance()
                            .encrypt(SparkFHE.getInstance().encode(String.valueOf(subArray[k][j]))).toString();
                }
            }
            result.add(CtxtMatrices.dense(subArray.length, subArray[0].length, values));
        }

        return result;
    }

    private static List<DenseVector> getList(double[] arr, int slices) {
        int numRows = arr.length;
        int chunk = (numRows % slices == 0)? numRows/slices: numRows/slices+1;
        List<DenseVector> result = new ArrayList<>();

        for (int i = 0; i < numRows; i+=chunk) {
            double [] subArr = Arrays.copyOfRange(arr, i, Math.min(numRows, i+chunk));
            result.add(new DenseVector(subArr));
        }

        return result;
    }

    private static List<CtxtDenseVector> getListCtxt(double[] arr, int slices) {
        int numRows = arr.length;
        int chunk = (numRows % slices == 0)? numRows/slices: numRows/slices+1;
        List<CtxtDenseVector> result = new ArrayList<>();

        for (int i = 0; i < numRows; i+=chunk) {
            double [] subArr = Arrays.copyOfRange(arr, i, Math.min(numRows, i+chunk));
            String [] values = new String[subArr.length];
            for (int j = 0; j < subArr.length; j++) {
                values[j] = SparkFHE.getInstance()
                        .encrypt(SparkFHE.getInstance().encode(String.valueOf(subArr[j]))).toString();
            }
            result.add(new CtxtDenseVector(values));
        }

        return result;
    }

    private static DenseVector fit(JavaPairRDD<Matrix, DenseVector> data, double alpha, int iters) {
        long t1 = System.currentTimeMillis();
        DenseVector weights = new DenseVector(new double[3]);
        for (int e = 1; e <= iters; e++) {
            double [] delta = data
            .map(tuple -> {
                Matrix x = tuple._1;
                DenseVector y = tuple._2;
                DenseVector g = sigmoid(x.multiply(weights));

                double [] errorValues = new double[y.size()];
                for (int i = 0; i < errorValues.length; i++) {
                    errorValues[i] = g.apply(i) - y.apply(i);
                }
                DenseVector errors = new DenseVector(errorValues);

                return x.transpose().multiply(errors).values();
            })
            .reduce((a, b) -> {
                for (int i = 0; i < a.length; i++) {
                    a[i] += b[i];
                }
                return a;
            });

            double [] weightValues = weights.values();
            for (int i = 0; i < weightValues.length; i++) {
                weightValues[i] -= (alpha/NROWS) * delta[i];
            }

            if (e % (iters/10) == 0) System.out.println("epoch: " + e + " -> " + weights);
        }
        System.out.printf("Time taken to train: %d ms\n", System.currentTimeMillis()-t1);

        return weights;
    }

    private static DenseVector fitNesterov(JavaPairRDD<Matrix, DenseVector> data, double gamma, double alpha, int iters) {
        long t1 = System.currentTimeMillis();
        DenseVector weights = new DenseVector(new double[3]);
        for (int e = 1; e <= iters; e++) {
            double [] delta = data
                    .map(tuple -> {
                        Matrix x = tuple._1;
                        DenseVector y = tuple._2;
                        DenseVector g = sigmoidPoly(x.multiply(weights), 7);

                        double [] errorValues = new double[y.size()];
                        for (int i = 0; i < errorValues.length; i++) {
                            errorValues[i] = g.apply(i) - y.apply(i);
                        }
                        DenseVector errors = new DenseVector(errorValues);

                        return x.transpose().multiply(errors).values();
                    })
                    .reduce((a, b) -> {
                        for (int i = 0; i < a.length; i++) {
                            a[i] += b[i];
                        }
                        return a;
                    });

            double [] weightValues = weights.values();
            for (int i = 0; i < weightValues.length; i++) {
                weightValues[i] -= (alpha/NROWS) * delta[i];
            }

            if (e % (iters/10) == 0) System.out.println("epoch: " + e + " -> " + weights);
        }
        System.out.printf("Time taken to train: %d ms\n", System.currentTimeMillis()-t1);

        return weights;
    }

    private static void accuracy(JavaPairRDD<Matrix, DenseVector> data, DenseVector weights, int rows) {
        int totalMatches = data.map(tuple -> {
           Matrix x = tuple._1;
           DenseVector y = tuple._2;
           DenseVector g = sigmoid(x.multiply(weights));
           int matches = 0;
           double[] values = g.values();
           for (int i = 0; i < values.length; i++) {
               values[i] = values[i] >= 0.5? 1.0: 0.0;
               values[i] -= y.apply(i);
               if (values[i] == 0) matches++;
           }
           return matches;
        })
        .reduce(Integer::sum);

        System.out.printf("Accuracy: %f\n", (double)(totalMatches)/rows);
    }

    private static void fitCtxt(JavaPairRDD<CtxtMatrix, CtxtDenseVector> data, double alpha, int iters) {
        long t1 = System.currentTimeMillis();
        String lr = SparkFHE.getInstance().encrypt(SparkFHE.getInstance().encode(String.valueOf(alpha/NROWS))).toString();
        String zero = SparkFHE.getInstance().encrypt(SparkFHE.getInstance().encode("0")).toString();
        String [] zeros = new String[X[0].length];
        Arrays.fill(zeros, zero);
        CtxtDenseVector weights = new CtxtDenseVector(zeros);
        for (int e = 1; e <= iters; e++) {
            String [] delta = data
                    .map(tuple -> {
                        CtxtMatrix x = tuple._1;
                        CtxtDenseVector y = tuple._2;
                        CtxtDenseVector g = sigmoid(x.multiply(weights));

                        String [] errorValues = new String[y.size()];
                        for (int i = 0; i < errorValues.length; i++) {
                            errorValues[i] = SparkFHE.getInstance().fhe_subtract(g.apply(i), y.apply(i));
                        }
                        CtxtDenseVector errors = new CtxtDenseVector(errorValues);

//                        System.out.println("Map complete...");
                        return x.transpose().multiply(errors).values();
                    })
                    .reduce((a, b) -> {
                        for (int i = 0; i < a.length; i++) {
                            a[i] = SparkFHE.getInstance().fhe_add(a[i], b[i]);
                        }
//                        System.out.println("Reduce complete");
                        return a;
                    });

            String [] weightValues = weights.values();
            for (int i = 0; i < weightValues.length; i++) {
                weightValues[i] = SparkFHE.getInstance().fhe_subtract(
                        weightValues[i],
                        SparkFHE.getInstance().fhe_multiply(lr, delta[i])
                );
            }
//            System.out.println("Weights updated...");

            if (e % (iters/10) == 0) {
                System.out.print("epoch: " + e + " -> ");
                decryptAndPrintCtxtDenseVector(weights);
            }
        }
        System.out.printf("Time taken to train: %d ms\n", System.currentTimeMillis()-t1);
    }

    private static void decryptAndPrintCtxtDenseVector(CtxtVector vector) {
        List<Double> result = new ArrayList<>();
        for (int i = 0; i < vector.size(); i++) {
            String encStr = vector.apply(i);
            DoubleVector output_vec = new DoubleVector();
            SparkFHE.getInstance().decode(output_vec, SparkFHE.getInstance().decrypt(new Ciphertext(encStr)));
            result.add(output_vec.get(0));
        }
        System.out.println(result);
    }

    private static void runPlainTextExample(Split split, JavaSparkContext jsc) {
        System.out.println("=========== Running Logistic Regression Example on PLain text ==========");
        JavaRDD<Matrix> trainXRdd = jsc.parallelize(getList(split.trainX, SLICES));
        JavaRDD<DenseVector> trainYRdd = jsc.parallelize(getList(split.trainY, SLICES));
        JavaPairRDD<Matrix, DenseVector> dataRDD = trainXRdd.zip(trainYRdd);

        DenseVector weights = fit(dataRDD, 0.5, 10000);

        JavaRDD<Matrix> testXRdd = jsc.parallelize(getList(split.testX, SLICES));
        JavaRDD<DenseVector> testYRdd = jsc.parallelize(getList(split.testY, SLICES));
        JavaPairRDD<Matrix, DenseVector> testRDD = testXRdd.zip(testYRdd);

        accuracy(testRDD, weights, split.testLength);
    }

    private static void runCtxtExample(Split split, JavaSparkContext jsc) {
        System.out.println("=========== Running Logistic Regression Example on Cipher text ==========");
        JavaRDD<CtxtMatrix> XRddCtxt = jsc.parallelize(getListCtxt(split.trainX, SLICES));
        JavaRDD<CtxtDenseVector> yRddCtxt = jsc.parallelize(getListCtxt(split.trainY, SLICES));
        JavaPairRDD<CtxtMatrix, CtxtDenseVector> dataRDDCtxt = XRddCtxt.zip(yRddCtxt);

        fitCtxt(dataRDDCtxt, 0.5, 10);
    }

    public static void main(String[] args) {
        String filePath = "/home/skd/Documents/SparkFHE-Examples/data/exam.csv";
        setXy(filePath);
        minMaxScaler(X);

        String scheme="", library = "", pk="", sk="";
        // The variable slices represent the number of time a task is split up
        int slices=2;

        // Create a SparkConf that loads defaults from system properties and the classpath
        SparkConf sparkConf = new SparkConf();
        //Provides the Spark driver application a name for easy identification in the Spark or Yarn UI
        sparkConf.setAppName("LogisticRegressionExample");

        // Decide whether to run the task locally or on the clusters
        Config.setExecutionEnvironment(args[0]);
        switch (Config.currentExecutionEnvironment) {
            case CLUSTER:
                slices = Integer.parseInt(args[0]);
                Config.set_HDFS_NAME_NODE(args[1]);
                library = args[2];
                scheme = args[3];
                pk = args[4];
                sk = args[5];
                break;
            case LOCAL:
                sparkConf.setMaster("local");
                library = args[1];
                scheme = args[2];
                pk = args[3];
                sk = args[4];
                break;
            default:
                break;
        }
        System.out.println("CURRENT_DIRECTORY = "+Config.get_current_directory());

        // Creating a session to Spark. The session allows the creation of the
        // various data abstractions such as RDDs, DataFrame, and more.
        SparkSession spark = SparkSession.builder().config(sparkConf).getOrCreate();

        // Creating spark context which allows the communication with worker nodes
        JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext());
        jsc.setLogLevel("ERROR");

        SparkFHEPlugin.setup();
        // create SparkFHE object
        SparkFHE.init(library, scheme, pk, sk);

        Broadcast<String> pk_b = jsc.broadcast(pk);
        Broadcast<String> sk_b = jsc.broadcast(sk);


        Split split = trainTestSplit(0.6);

//        runPlainTextExample(split, jsc);
        runCtxtExample(split, jsc);

        // Stop existing spark context
        jsc.close();

        // Stop existing spark session
        spark.close();
    }
}
