/**
 * Copyright 2013 Crypto Workshop Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cryptoworkshop.ximix.common.asn1.message;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERUTF8String;

/**
 * Carrier message for a node's sequence number in the threshold scheme and the node's name.
 */
public class ParticipantMessage
    extends ASN1Object
{
    private final String name;
    private final int sequenceNo;

    /**
     * Base constructor.
     *
     * @param sequenceNo the sequence number associated with this participant.
     * @param name the name of the node that identifies this participant.
     */
    public ParticipantMessage(int sequenceNo, String name)
    {
        this.sequenceNo = sequenceNo;
        this.name = name;
    }

    private ParticipantMessage(ASN1Sequence seq)
    {
        this.sequenceNo = ASN1Integer.getInstance(seq.getObjectAt(0)).getValue().intValue();
        this.name = DERUTF8String.getInstance(seq.getObjectAt(1)).getString();
    }

    public static final ParticipantMessage getInstance(Object o)
    {
        if (o instanceof ParticipantMessage)
        {
            return (ParticipantMessage)o;
        }
        else if (o != null)
        {
            return new ParticipantMessage(ASN1Sequence.getInstance(o));
        }

        return null;
    }

    @Override
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(new ASN1Integer(sequenceNo));
        v.add(new DERUTF8String(name));

        return new DERSequence(v);
    }

    public int getSequenceNo()
    {
        return sequenceNo;
    }

    public String getName()
    {
        return name;
    }
}
