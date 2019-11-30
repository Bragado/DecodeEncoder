package com.example.decoderencoder.library.output;

import com.example.decoderencoder.library.core.encoder.EncoderBuffer;
import com.example.decoderencoder.library.muxer.MuxerInput;
import com.example.decoderencoder.library.network.Allocation;
import com.example.decoderencoder.library.network.Allocator;
import com.example.decoderencoder.library.source.SampleQueue;
import com.example.decoderencoder.library.source.TrackGroup;
import com.example.decoderencoder.library.util.C;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A simplified version of SampleQueue
 */
public class TrackQueue implements MuxerInput {

    final Allocator allocator;
    final int allocationLength;
    SampleQueue.AllocationNode readAllocationNode;
    SampleQueue.AllocationNode writeAllocationNode;
    private long totalBytesWritten;
    TrackGroup trackGroup;


    public TrackQueue(TrackGroup trackGroup, Allocator allocator) {
        this.allocator = allocator;
        allocationLength = allocator.getIndividualAllocationLength();
        readAllocationNode = new SampleQueue.AllocationNode(0, allocator.getIndividualAllocationLength());
        writeAllocationNode = readAllocationNode;
        totalBytesWritten = 0;
        this.trackGroup = trackGroup;
    }


    /**
     * Called before writing sample data to {@link #writeAllocationNode}. May cause
     * {@link #writeAllocationNode} to be initialized.
     *
     * @param length The number of bytes that the caller wishes to write.
     * @return The number of bytes that the caller is permitted to write, which may be less than
     *     {@code length}.
     */
    private int preAppend(int length) {
        if (!writeAllocationNode.wasInitialized) {
            writeAllocationNode.initialize(allocator.allocate(),
                    new SampleQueue.AllocationNode(writeAllocationNode.endPosition, allocationLength));
        }
        return Math.min(length, (int) (writeAllocationNode.endPosition - totalBytesWritten));
    }
    /**
     * Called after writing sample data. May cause {@link #writeAllocationNode} to be advanced.
     *
     * @param length The number of bytes that were written.
     */
    private void postAppend(int length) {
        totalBytesWritten += length;
        if (totalBytesWritten == writeAllocationNode.endPosition) {
            writeAllocationNode = writeAllocationNode.next;
        }
    }


    @Override
    public int sampleData(EncoderBuffer outputBuffer, int length) throws IOException, InterruptedException {
        int remaining_bytes = length;

        while(remaining_bytes > 0) {
            length = preAppend(length);
            int offset = writeAllocationNode.translateOffset(totalBytesWritten);
            outputBuffer.data.put(writeAllocationNode.allocation.data, offset, length);
            postAppend(length);
            remaining_bytes -= length;
        }
        return length;
    }

    /**
     * Reads data from the front of the rolling buffer.
     *
     * @param absolutePosition The absolute position from which data should be read.
     * @param target The buffer into which data should be written.
     * @param length The number of bytes to read.
     */
    private void readData(long absolutePosition, ByteBuffer target, int length) {
        advanceReadTo(absolutePosition);
        int remaining = length;
        while (remaining > 0) {
            int toCopy = Math.min(remaining, (int) (readAllocationNode.endPosition - absolutePosition));
            Allocation allocation = readAllocationNode.allocation;
            target.put(allocation.data, readAllocationNode.translateOffset(absolutePosition), toCopy);
            remaining -= toCopy;
            absolutePosition += toCopy;
            if (absolutePosition == readAllocationNode.endPosition) {
                allocator.release(readAllocationNode.allocation);
                readAllocationNode = readAllocationNode.next;
            }
        }
    }


    /**
     * Advances {@link #readAllocationNode} to the specified absolute position.
     *
     * @param absolutePosition The position to which {@link #readAllocationNode} should be advanced.
     */
    private void advanceReadTo(long absolutePosition) {
        while (absolutePosition >= readAllocationNode.endPosition) {
            readAllocationNode = readAllocationNode.next;
        }
    }

}
