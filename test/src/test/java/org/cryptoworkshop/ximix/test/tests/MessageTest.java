package org.cryptoworkshop.ximix.test.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

import junit.framework.TestCase;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEROutputStream;
import org.cryptoworkshop.ximix.common.asn1.message.BigIntegerMessage;
import org.cryptoworkshop.ximix.common.asn1.message.BoardErrorStatusMessage;
import org.cryptoworkshop.ximix.common.asn1.message.CommandMessage;
import org.cryptoworkshop.ximix.common.asn1.message.PermuteAndMoveMessage;
import org.junit.Test;

/**
 * Basic testing concerning messaging.
 */
public class MessageTest extends TestCase
{

    @Test
    public void testCommandMessageRoundTrip()
        throws Exception
    {

        CommandMessage msg = new CommandMessage(CommandMessage.Type.ACTIVATE_BOARD, new ASN1Integer(1));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DEROutputStream derOut = new DEROutputStream(bos);

        derOut.writeObject(msg.toASN1Primitive());

        ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
        ASN1InputStream din = new ASN1InputStream(bin);

        CommandMessage res = CommandMessage.getInstance(din.readObject());

        TestCase.assertEquals(msg.getPayload(), res.getPayload());
        TestCase.assertEquals(msg.getType(), res.getType());
    }

    @Test
    public void testPermuteAndMoveRoundTrip_1()
        throws Exception
    {

        PermuteAndMoveMessage msg = new PermuteAndMoveMessage(1, "Cat", 0, "Doc", "Fish", "Rabbit");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DEROutputStream derOut = new DEROutputStream(bos);

        derOut.writeObject(msg.toASN1Primitive());

        ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
        ASN1InputStream din = new ASN1InputStream(bin);

        PermuteAndMoveMessage res = PermuteAndMoveMessage.getInstance(din.readObject());

        TestCase.assertEquals(msg.getBoardName(), res.getBoardName());
        TestCase.assertEquals(msg.getDestinationNode(), res.getDestinationNode());
        TestCase.assertEquals(msg.getKeyID(), res.getKeyID());
        TestCase.assertEquals(msg.getTransformName(), res.getTransformName());

    }

    @Test
    public void testPermuteAndMoveRoundTrip_2()
        throws Exception
    {

        PermuteAndMoveMessage msg = new PermuteAndMoveMessage(1, "Cat", 0, "Doc", null, "Rabbit");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DEROutputStream derOut = new DEROutputStream(bos);

        derOut.writeObject(msg.toASN1Primitive());

        ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
        ASN1InputStream din = new ASN1InputStream(bin);

        PermuteAndMoveMessage res = PermuteAndMoveMessage.getInstance(din.readObject());

        TestCase.assertEquals(msg.getBoardName(), res.getBoardName());
        TestCase.assertEquals(msg.getDestinationNode(), res.getDestinationNode());
        TestCase.assertEquals(msg.getKeyID(), res.getKeyID());
        TestCase.assertEquals(msg.getTransformName(), res.getTransformName());

    }

    @Test
    public void testBoardErrorStatusMessage_1()
        throws Exception
    {
        BoardErrorStatusMessage msg = new BoardErrorStatusMessage("foo", BoardErrorStatusMessage.Status.NOT_SHUFFLE_LOCKED);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DEROutputStream derOut = new DEROutputStream(bos);

        derOut.writeObject(msg.toASN1Primitive());

        ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
        ASN1InputStream din = new ASN1InputStream(bin);

        BoardErrorStatusMessage res = BoardErrorStatusMessage.getInstance(ASN1TaggedObject.getInstance(din.readObject()).getObject());


        TestCase.assertEquals(msg.getBoardName(), res.getBoardName());
        TestCase.assertEquals(msg.getStatus(), res.getStatus());
   }

    @Test
    public void testBigIntegerMessage_1()
        throws Exception
    {
        BigIntegerMessage msg = new BigIntegerMessage(BigInteger.valueOf(Long.MAX_VALUE));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DEROutputStream derOut = new DEROutputStream(bos);

        derOut.writeObject(msg.toASN1Primitive());

        ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
        ASN1InputStream din = new ASN1InputStream(bin);

        BigIntegerMessage res = BigIntegerMessage.getInstance(din.readObject());

        TestCase.assertEquals(msg.getValue(), res.getValue());
    }


    public void testECPointMessage() throws Exception {


    }

}
