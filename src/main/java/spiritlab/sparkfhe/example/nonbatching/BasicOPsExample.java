//
// Copyright SpiRITlab - The SparkFHE project.
// https://github.com/SpiRITlab
//

package spiritlab.sparkfhe.example.nonbatching;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.spiritlab.sparkfhe.SparkFHEPlugin;
import org.apache.spark.sql.*;
import spiritlab.sparkfhe.api.*;
import spiritlab.sparkfhe.example.Config;

/**
 * This is an example for SparkFHE project. Created to test the functionality
 * of the underlying C++ APIs. A few simple functions are invoked via lambda.
 */
public class BasicOPsExample {

    private static String CTXT_0_FILE;
    private static String CTXT_1_FILE;


    public static void test_basic_op() {
        // Testing the addition function
        System.out.println("ADD(1, 0):"+String.valueOf(1+0));
        // Testing the multiplication function
        System.out.println("MUL(1, 0):"+String.valueOf(1*0));
        // Testing the substraction function
        System.out.println("SUB(1, 0):"+String.valueOf(1-0));
    }

    /**
     * This method performs the basic HE operations on ciphertexts and print out the results
     * @param spark spark session
     * @param slices the number of time a task is split up
     * @param pk_b broadcast variable for public key
     * @param sk_b broadcast variable for secret key
     */
    public static void test_FHE_basic_op(SparkSession spark, int slices, String library, String scheme, Broadcast<String> pk_b,
                                         Broadcast<String> sk_b) {
        /* Spark example for FHE calculations */
        // Encoders are created for Java beans
        Encoder<SerializedCiphertext> ctxtJSONEncoder = Encoders.bean(SerializedCiphertext.class);

        // https://spark.apache.org/docs/latest/sql-programming-guide.html#untyped-dataset-operations-aka-dataframe-operations
        // Create dataset with json file.
        // if CtxtString a row? Dataset<Row> is the Dataframe in Java
        JavaRDD<SerializedCiphertext> ctxt_zero_rdd = spark.read().json(CTXT_0_FILE).as(ctxtJSONEncoder).javaRDD();
        System.out.println("Ciphertext Zero:"+SparkFHE.getInstance().decrypt(ctxt_zero_rdd.first().getCtxt(), true));

        JavaRDD<SerializedCiphertext> ctxt_one_rdd = spark.read().json(CTXT_1_FILE).as(ctxtJSONEncoder).javaRDD();
        System.out.println("Ciphertext One:"+SparkFHE.getInstance().decrypt(ctxt_one_rdd.first().getCtxt(), true));

        // combine both rdds as a pair
        JavaPairRDD<SerializedCiphertext, SerializedCiphertext> Combined_ctxt_RDD = ctxt_one_rdd.zip(ctxt_zero_rdd).cache();

        // call homomorphic addition operators on the rdds
        JavaRDD<SerializedCiphertext> Addition_ctxt_RDD = Combined_ctxt_RDD.map(tuple -> {
            // we need to load the shared library and init a copy of SparkFHE on the executor
            SparkFHEPlugin.setup();
            SparkFHE.init(library, scheme, pk_b.getValue(), sk_b.getValue());
            return new SerializedCiphertext(SparkFHE.getInstance().fhe_add(tuple._1().getCtxt(), tuple._2().getCtxt()));
        });
        System.out.println("Homomorphic Addition:"+ SparkFHE.getInstance().decrypt(Addition_ctxt_RDD.first().getCtxt(), true));

        // call homomorphic multiply operators on the rdds
        JavaRDD<SerializedCiphertext> Multiplication_ctxt_RDD = Combined_ctxt_RDD.map(tuple -> {
            // we need to load the shared library and init a copy of SparkFHE on the executor
            SparkFHEPlugin.setup();
            SparkFHE.init(library, scheme, pk_b.getValue(), sk_b.getValue());
            return new SerializedCiphertext(SparkFHE.getInstance().fhe_multiply(tuple._1().getCtxt(), tuple._2().getCtxt()));
        });
        System.out.println("Homomorphic Multiplication:"+ SparkFHE.getInstance().decrypt(Multiplication_ctxt_RDD.first().getCtxt(), true));

        // call homomorphic subtraction operators on the rdds
        JavaRDD<SerializedCiphertext> Subtraction_ctxt_RDD = Combined_ctxt_RDD.map(tuple -> {
            // we need to load the shared library and init a copy of SparkFHE on the executor
            SparkFHEPlugin.setup();
            SparkFHE.init(library, scheme, pk_b.getValue(), sk_b.getValue());
            return new SerializedCiphertext(SparkFHE.getInstance().fhe_subtract(tuple._1().getCtxt(), tuple._2().getCtxt()));
        });
        System.out.println("Homomorphic Subtraction:"+SparkFHE.getInstance().decrypt(Subtraction_ctxt_RDD.first().getCtxt(), true));
    }

    /**
     * This method performs the basic HE operations on vectors and print out the results
     * @param spark spark session
     * @param slices the number of time a task is split up
     * @param pk_b broadcast variable for public key
     * @param sk_b broadcast variable for secret key
     */
    public static void test_FHE_vector_op(SparkSession spark, int slices, String library, String scheme, Broadcast<String> pk_b,
                                          Broadcast<String> sk_b) {
        /* Spark example for FHE calculations on vectors */
        // Encoders are created for Java beans
        Encoder<SerializedCiphertext> ctxtJSONEncoder = Encoders.bean(SerializedCiphertext.class);

        // FHE Ops on vectors
        String packed_a_ctxt = Config.get_records_directory()+"/vec_a_"+String.valueOf(Config.NUM_OF_VECTOR_ELEMENTS)+"_"+SparkFHE.getInstance().generate_crypto_params_suffix()+ ".jsonl";
        String packed_b_ctxt = Config.get_records_directory()+"/vec_b_"+String.valueOf(Config.NUM_OF_VECTOR_ELEMENTS)+"_"+SparkFHE.getInstance().generate_crypto_params_suffix()+ ".jsonl";

        // https://spark.apache.org/docs/latest/sql-programming-guide.html#untyped-dataset-operations-aka-dataframe-operations
        // Create dataset with json file. See http://jsonlines.org
        JavaRDD<SerializedCiphertext> ctxt_a_rdd = spark.read().json(packed_a_ctxt).as(ctxtJSONEncoder).javaRDD();
        JavaRDD<SerializedCiphertext> ctxt_b_rdd = spark.read().json(packed_b_ctxt).as(ctxtJSONEncoder).javaRDD();

        // combine both rdds as a pair
        JavaPairRDD<SerializedCiphertext, SerializedCiphertext> combined_ctxt_rdd = ctxt_a_rdd.zip(ctxt_b_rdd);
        System.out.println("combined_ctxt_rdd.count() = " + combined_ctxt_rdd.count());

        // call homomorphic addition operators on the rdds
        JavaRDD<SerializedCiphertext> Addition_ctxt_RDD = combined_ctxt_rdd.map(tuple -> {
            // we need to load the shared library and init a copy of SparkFHE on the executor
            SparkFHEPlugin.setup();
            SparkFHE.init(library, scheme, pk_b.getValue(), sk_b.getValue());
            return new SerializedCiphertext(SparkFHE.getInstance().fhe_add(tuple._1().getCtxt(), tuple._2().getCtxt()));
        });

        System.out.println("Vector Homomorphic Addition");
        Addition_ctxt_RDD.foreach(data -> {
            // we need to load the shared library and init a copy of SparkFHE on the executor
            SparkFHEPlugin.setup();
            SparkFHE.init(library, scheme, pk_b.getValue(), sk_b.getValue());
            System.out.println(SparkFHE.getInstance().decrypt(data.getCtxt(), true));
        });

        // call homomorphic multiply operators on the rdds
        JavaRDD<SerializedCiphertext> Multiplication_ctxt_RDD = combined_ctxt_rdd.map(tuple -> {
            // we need to load the shared library and init a copy of SparkFHE on the executor
            SparkFHEPlugin.setup();
            SparkFHE.init(library, scheme, pk_b.getValue(), sk_b.getValue());
            return new SerializedCiphertext(SparkFHE.getInstance().fhe_multiply(tuple._1().getCtxt(), tuple._2().getCtxt()));
        });
        System.out.println("Vector Homomorphic Multiplication");
        Multiplication_ctxt_RDD.foreach(data -> {
            // we need to load the shared library and init a copy of SparkFHE on the executor
            SparkFHEPlugin.setup();
            SparkFHE.init(library, scheme, pk_b.getValue(), sk_b.getValue());
            System.out.println(SparkFHE.getInstance().decrypt(data.getCtxt(), true));
        });

        // call homomorphic subtraction operators on the rdds
        JavaRDD<SerializedCiphertext> Subtraction_ctxt_RDD = combined_ctxt_rdd.map(tuple -> {
            // we need to load the shared library and init a copy of SparkFHE on the executor
            SparkFHEPlugin.setup();
            SparkFHE.init(library, scheme, pk_b.getValue(), sk_b.getValue());
            return new SerializedCiphertext(SparkFHE.getInstance().fhe_subtract(tuple._1().getCtxt(), tuple._2().getCtxt()));
        });
        System.out.println("Vector Homomorphic Subtraction");
        Subtraction_ctxt_RDD.foreach(data -> {
            // we need to load the shared library and init a copy of SparkFHE on the executor
            SparkFHEPlugin.setup();
            SparkFHE.init(library, scheme, pk_b.getValue(), sk_b.getValue());
            System.out.println(SparkFHE.getInstance().decrypt(data.getCtxt(), true));
        });
    }


    public static void main(String[] args) {
        String scheme="", library = "", pk="", sk="";

        // The variable slices represent the number of time a task is split up
        int slices = 2;
        // Create a SparkConf that loads defaults from system properties and the classpath
        SparkConf sparkConf = new SparkConf();
        //Provides the Spark driver application a name for easy identification in the Spark or Yarn UI
        sparkConf.setAppName("BasicOPsExample");

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

        // Note, the following loading of shared library and init are done on driver only. We need to do the same on the executors.
        // Load C++ shared library
        SparkFHEPlugin.setup();
        // Create SparkFHE object with FHE library
        SparkFHE.init(library, scheme, pk, sk);


        CTXT_0_FILE = Config.get_records_directory() + "/ptxt_long_0_"+ SparkFHE.getInstance().generate_crypto_params_suffix()+ ".jsonl";
        CTXT_1_FILE = Config.get_records_directory() +"/ptxt_long_1_"+SparkFHE.getInstance().generate_crypto_params_suffix()+ ".jsonl";

        Broadcast<String> pk_b = jsc.broadcast(pk);
        Broadcast<String> sk_b = jsc.broadcast(sk);


        // Testing and printing the addition function
        System.out.println("testing 1+1=" +String.valueOf(1+1));

        // Start testing the basic operations on plain text, such as addition, subtraction, and multiply.
        test_basic_op();

        // String testing the basic operations in HE libraries on encrypted data, such as addition, subtraction, and multiply.
        test_FHE_basic_op(spark, slices, library, scheme, pk_b, sk_b);

        // Testing the basic operations in HE libraries on encrypted vectors, such as addition, subtraction, and multiply.
        test_FHE_vector_op(spark, slices, library, scheme, pk_b, sk_b);

        // Normally, the Spark web UI at http://127.0.0.1:4040 will be shutdown after the experiment run.
        // Uncomment the following block of code to paused the shutdown so that you have a chance to check the Spark web UI.
//        try {
//            System.out.println("Paused to allow checking the Spark server log, press enter to continue.");
//            System.in.read();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // Stop existing spark context
        jsc.close();

        // Stop existing spark session
        spark.close();
    }

}
