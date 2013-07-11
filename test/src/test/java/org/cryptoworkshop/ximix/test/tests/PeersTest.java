package org.cryptoworkshop.ximix.test.tests;

import junit.framework.TestCase;
import org.bouncycastle.crypto.ec.ECElGamalEncryptor;
import org.bouncycastle.crypto.ec.ECPair;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.math.ec.ECPoint;
import org.cryptoworkshop.ximix.common.board.asn1.PairSequence;
import org.cryptoworkshop.ximix.common.board.asn1.PointSequence;
import org.cryptoworkshop.ximix.common.operation.Operation;
import org.cryptoworkshop.ximix.common.service.KeyType;
import org.cryptoworkshop.ximix.crypto.KeyGenerationOptions;
import org.cryptoworkshop.ximix.crypto.client.KeyGenerationService;
import org.cryptoworkshop.ximix.mixnet.DownloadOptions;
import org.cryptoworkshop.ximix.mixnet.admin.CommandService;
import org.cryptoworkshop.ximix.mixnet.admin.DownloadOperationListener;
import org.cryptoworkshop.ximix.mixnet.client.UploadService;
import org.cryptoworkshop.ximix.node.XimixNode;
import org.cryptoworkshop.ximix.registrar.XimixRegistrar;
import org.cryptoworkshop.ximix.registrar.XimixRegistrarFactory;
import org.cryptoworkshop.ximix.test.node.NodeTestUtil;
import org.cryptoworkshop.ximix.test.node.ResourceAnchor;
import org.cryptoworkshop.ximix.test.node.SquelchingThrowableHandler;
import org.cryptoworkshop.ximix.test.node.ValueObject;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.cryptoworkshop.ximix.test.node.NodeTestUtil.getXimixNode;

/**
 * Tests involving peers and incorrect numbers of peers.
 */
public class PeersTest
{

    /**
     * Test a network failure where 5 nodes are used for encryption but one fails before decryption.
     * Decryption should be successful.
     *
     * @throws Exception
     */
    @Test
    public void testInsufficientPeers_5_Enc_4_Dec()
        throws Exception
    {
        SquelchingThrowableHandler handler = new SquelchingThrowableHandler();

        handler.setPrintOnly(true);
        //handler.squelchType(SocketException.class);


        //
        // Set up nodes.
        //

        XimixNode nodeOne = getXimixNode("/conf/mixnet.xml", "/conf/node1.xml", handler);
        NodeTestUtil.launch(nodeOne, true);

        XimixNode nodeTwo = getXimixNode("/conf/mixnet.xml", "/conf/node2.xml", handler);
        NodeTestUtil.launch(nodeTwo, false);

        XimixNode nodeThree = getXimixNode("/conf/mixnet.xml", "/conf/node3.xml", handler);
        NodeTestUtil.launch(nodeThree, false);

        XimixNode nodeFour = getXimixNode("/conf/mixnet.xml", "/conf/node4.xml", handler);
        NodeTestUtil.launch(nodeFour, false);

        XimixNode nodeFive = getXimixNode("/conf/mixnet.xml", "/conf/node5.xml", handler);
        NodeTestUtil.launch(nodeFive, false);


        SecureRandom random = new SecureRandom();

        XimixRegistrar adminRegistrar = XimixRegistrarFactory.createAdminServiceRegistrar(ResourceAnchor.load("/conf/mixnet.xml"));

        KeyGenerationService keyGenerationService = adminRegistrar.connect(KeyGenerationService.class);

        KeyGenerationOptions keyGenOptions = new KeyGenerationOptions.Builder(KeyType.EC_ELGAMAL, "secp256r1")
            .withThreshold(4)
            .withNodes("A", "B", "C", "D", "E")
            .build();

        byte[] encPubKey = keyGenerationService.generatePublicKey("ECKEY", keyGenOptions);

        UploadService client = adminRegistrar.connect(UploadService.class);

        final ECPublicKeyParameters pubKey = (ECPublicKeyParameters)PublicKeyFactory.createKey(encPubKey);

        final ECElGamalEncryptor encryptor = new ECElGamalEncryptor();

        encryptor.init(pubKey);


        //
        // Set up plain text and upload encrypted pair.
        //

        int numberOfPoints = 1; // Adjust number of points to test here.


        final ECPoint[] plainText1 = new ECPoint[numberOfPoints];
        final ECPoint[] plainText2 = new ECPoint[numberOfPoints];


        //
        // Encrypt and submit.
        //
        for (int i = 0; i < plainText1.length; i++)
        {
            plainText1[i] = generatePoint(pubKey.getParameters(), random);
            plainText2[i] = generatePoint(pubKey.getParameters(), random);

            PairSequence encrypted = new PairSequence(new ECPair[]{encryptor.encrypt(plainText1[i]), encryptor.encrypt(plainText2[i])});

            client.uploadMessage("FRED", encrypted.getEncoded());
        }

        CommandService commandService = adminRegistrar.connect(CommandService.class);


        //
        // Here we shut down on node ('E'), the remainder of the test should still pass.
        //


        TestCase.assertTrue("Node 5, failed to shutdown.",nodeFive.shutdown(10,TimeUnit.SECONDS));



        final ECPoint[] resultText1 = new ECPoint[plainText1.length];
        final ECPoint[] resultText2 = new ECPoint[plainText2.length];
        final ValueObject<Boolean> downloadBoardCompleted = new ValueObject<Boolean>(false);
        final ValueObject<Boolean> downloadBoardFailed = new ValueObject<Boolean>(false);
        final CountDownLatch encryptLatch = new CountDownLatch(1);
        final ValueObject<Thread> decryptThread = new ValueObject<>();

        Operation<DownloadOperationListener> op = commandService.downloadBoardContents(
            "FRED",
            new DownloadOptions.Builder()
                .withKeyID("ECKEY")
                .withThreshold(3)
                .withNodes("A", "B", "C","D", "E").build(),
            new DownloadOperationListener()
            {
                int counter = 0;

                @Override
                public void messageDownloaded(byte[] message)
                {
                    PointSequence decrypted = PointSequence.getInstance(pubKey.getParameters().getCurve(), message);
                    resultText1[counter] = decrypted.getECPoints()[0];
                    resultText2[counter++] = decrypted.getECPoints()[1];
                }

                @Override
                public void completed()
                {
                    downloadBoardCompleted.set(true);
                    decryptThread.set(Thread.currentThread());
                    encryptLatch.countDown();
                }

                @Override
                public void failed(String errorObject)
                {
                    downloadBoardFailed.set(true);
                    encryptLatch.countDown();
                }
            });


        TestCase.assertTrue(encryptLatch.await(20, TimeUnit.SECONDS));


        TestCase.assertNotSame("Failed and complete must be different.", downloadBoardFailed.get(), downloadBoardCompleted.get());
        TestCase.assertTrue("Complete method called in DownloadOperationListener", downloadBoardCompleted.get());
        TestCase.assertFalse("Not failed.", downloadBoardFailed.get());

//        TestCase.assertEquals("Shuffle and decrypt threads different.",decryptThread.get(), shuffleThread.get());


        //
        // Validate result points against plainText points.
        //

        for (int t = 0; t < plainText1.length; t++)
        {
            TestCase.assertTrue(plainText1[t].equals(resultText1[t]));
            TestCase.assertTrue(plainText2[t].equals(resultText2[t]));
        }


        NodeTestUtil.shutdownNodes();
        client.shutdown();
        commandService.shutdown();


    }


    private static BigInteger getRandomInteger(BigInteger n, SecureRandom rand)
    {
        BigInteger r;
        int maxbits = n.bitLength();
        do
        {
            r = new BigInteger(maxbits, rand);
        }
        while (r.compareTo(n) >= 0);
        return r;
    }

    private static ECPoint generatePoint(ECDomainParameters params, SecureRandom rand)
    {
        return params.getG().multiply(getRandomInteger(params.getN(), rand));
    }


}


//
// Perform shuffle.
//

//        Operation<ShuffleOperationListener> shuffleOp = commandService.doShuffleAndMove("FRED", new ShuffleOptions.Builder(MultiColumnRowTransform.NAME).setKeyID("ECKEY").build(), "A", "B", "C", "D", "E");
//
//        final CountDownLatch shufflerLatch = new CountDownLatch(1);
//
//        final ValueObject<Boolean> shuffleCompleted = new ValueObject<Boolean>(false);
//        final ValueObject<Boolean> shuffleFailed = new ValueObject<Boolean>(false);
//        final ValueObject<Thread> shuffleThread = new ValueObject<>();
//
//
//        shuffleOp.addListener(new ShuffleOperationListener()
//        {
//            @Override
//            public void completed()
//            {
//                shuffleCompleted.set(true);
//                shuffleThread.set(Thread.currentThread());
//                shufflerLatch.countDown();
//            }
//
//            @Override
//            public void failed(String errorObject)
//            {
//                shuffleFailed.set(true);
//                shufflerLatch.countDown();
//            }
//        });

//
// Fail if operation did not complete in the nominated time frame.
//
//        TestCase.assertTrue("Shuffle timed out.", shufflerLatch.await(20, TimeUnit.SECONDS));

//
// Check that failed and completed methods are exclusive.
//

//        TestCase.assertNotSame("Failed flag and completed flag must be different.", shuffleCompleted.get(), shuffleFailed.get());

//
// Check for success of shuffle.
//
//        TestCase.assertTrue(shuffleCompleted.get());

//
// Check that shuffle did not fail.
//
//        TestCase.assertFalse(shuffleFailed.get());