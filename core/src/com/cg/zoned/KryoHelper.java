package com.cg.zoned;

import com.cg.zoned.Player.Direction;
import com.cg.zoned.buffers.BufferClientConnect;
import com.cg.zoned.buffers.BufferDirections;
import com.cg.zoned.buffers.BufferGameEnd;
import com.cg.zoned.buffers.BufferGameStart;
import com.cg.zoned.buffers.BufferKickClient;
import com.cg.zoned.buffers.BufferMapData;
import com.cg.zoned.buffers.BufferNewMap;
import com.cg.zoned.buffers.BufferPlayerData;
import com.cg.zoned.buffers.BufferPlayerLeft;
import com.esotericsoftware.kryo.Kryo;

public class KryoHelper {
    public static void registerClasses(Kryo kryo) {
        kryo.register(BufferPlayerLeft.class);
        kryo.register(BufferClientConnect.class);
        kryo.register(BufferPlayerData.class);
        kryo.register(BufferDirections.class);
        kryo.register(BufferKickClient.class);
        kryo.register(BufferGameStart.class);
        kryo.register(BufferGameEnd.class);
        kryo.register(BufferMapData.class);
        kryo.register(BufferNewMap.class);
        kryo.register(Direction[].class);
        kryo.register(Direction.class);
        kryo.register(boolean[].class);
        kryo.register(String[].class);
        kryo.register(byte[].class);
        kryo.register(int[].class);
    }
}