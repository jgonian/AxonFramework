/*
 * Copyright (c) 2010-2012. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.eventhandling.io;


import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.EventMessage;
import org.axonframework.io.DefaultMessageDefinitions;
import org.axonframework.serializer.SerializedObject;
import org.axonframework.serializer.Serializer;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Writer that writes Event Messages onto a an OutputStream. The format of the message makes them suitable to be read
 * back in using a {@link EventMessageReader}. This writer distinguishes between DomainEventMessage and plain
 * EventMessage when writing. The reader will reconstruct an aggregate implementation for the same message type (i.e.
 * DomainEventMessage or EventMessage).
 *
 * @author Allard Buijze
 * @since 2.0
 */
public class EventMessageWriter {

    private final Serializer serializer;
    private final DataOutput out;

    /**
     * Creates a new EventMessageWriter writing data to the specified underlying <code>output</code>.
     *
     * @param output     the underlying output
     * @param serializer The serializer to deserialize payload and metadata with
     */
    public EventMessageWriter(DataOutput output, Serializer serializer) {
        this.out = output;
        this.serializer = serializer;
    }

    /**
     * Writes the given <code>eventMessage</code> to the underling output.
     *
     * @param eventMessage the EventMessage to write to the underlying output
     * @throws IOException when any exception occurs writing to the underlying stream
     */
    public void writeEventMessage(EventMessage eventMessage) throws IOException {
        if (DomainEventMessage.class.isInstance(eventMessage)) {
            out.writeByte(DefaultMessageDefinitions.DOMAIN_EVENT_MESSAGE.getTypeByte());
        } else {
            out.writeByte(DefaultMessageDefinitions.EVENT_MESSAGE.getTypeByte());
        }
        out.writeUTF(eventMessage.getIdentifier());
        out.writeUTF(eventMessage.getTimestamp().toString());
        if (eventMessage instanceof DomainEventMessage) {
            DomainEventMessage domainEventMessage = (DomainEventMessage) eventMessage;
            out.writeUTF(domainEventMessage.getAggregateIdentifier().toString());
            out.writeLong(domainEventMessage.getSequenceNumber());
        }
        SerializedObject<byte[]> serializedPayload = serializer.serialize(eventMessage.getPayload(), byte[].class);
        SerializedObject<byte[]> serializedMetaData = serializer.serialize(eventMessage.getMetaData(), byte[].class);

        out.writeUTF(serializedPayload.getType().getName());
        String revision = serializedPayload.getType().getRevision();
        out.writeUTF(revision == null ? "" : revision);
        out.writeInt(serializedPayload.getData().length);
        out.write(serializedPayload.getData());
        out.writeInt(serializedMetaData.getData().length);
        out.write(serializedMetaData.getData());
    }
}
