package com.cg.zoned;

import com.cg.zoned.Constants.Direction;
import com.cg.zoned.buffers.BufferClientConnect;
import com.cg.zoned.buffers.BufferDirections;
import com.cg.zoned.buffers.BufferGameStart;
import com.cg.zoned.buffers.BufferNewMap;
import com.cg.zoned.buffers.BufferPlayerData;
import com.cg.zoned.buffers.BufferPlayerDisconnected;
import com.cg.zoned.buffers.BufferServerRejectedConnection;
import com.esotericsoftware.kryo.Kryo;

public class KryoHelper {

    public static void registerClasses(Kryo kryo) {
        kryo.register(BufferServerRejectedConnection.class);
        kryo.register(BufferPlayerDisconnected.class);
        kryo.register(BufferClientConnect.class);
        kryo.register(BufferPlayerData.class);
        kryo.register(BufferDirections.class);
        kryo.register(BufferGameStart.class);
        kryo.register(BufferNewMap.class);
        kryo.register(Direction[].class);
        kryo.register(Direction.class);
        kryo.register(String[].class);
        kryo.register(int[].class);
    }
}