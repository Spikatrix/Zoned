package com.cg.zoned;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.cg.zoned.dataobjects.TeamData;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.maps.InvalidMapCharacter;
import com.cg.zoned.maps.InvalidMapDimensions;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.MapExtraParams;
import com.cg.zoned.maps.NoStartPositionsFound;
import com.cg.zoned.screens.ClientLobbyScreen;
import com.cg.zoned.screens.GameScreen;
import com.cg.zoned.screens.HostJoinScreen;
import com.cg.zoned.screens.MapStartPosScreen;
import com.cg.zoned.screens.ScreenObject;
import com.cg.zoned.screens.ServerLobbyScreen;
import com.cg.zoned.screens.VictoryScreen;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

/**
 * Class used to jump to a certain screen quickly during development and testing.
 * Use this from the {@link com.cg.zoned.screens.LoadingScreen} after everything has been loaded.
 */
public class ScreenHelper {
    private static final Random random = new Random();

    public static Screen initScreen(Class targetScreenClass, Zoned game) {
        if (!targetScreenClass.getSuperclass().equals(ScreenObject.class)) {
            throw new ClassCastException("Class to be initialized needs to be a subclass of ScreenObject");
        }

        try {
            return getScreenInstance(targetScreenClass, game);
        } catch (ReflectionException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Screen getScreenInstance(Class targetScreenClass, Zoned game) throws ReflectionException {
        // Checks below handle classes which require more than just the game argument for instantiation
        String className = targetScreenClass.getName();
        if (className.equals(ServerLobbyScreen.class.getName())) {
            return getServerLobbyScreen(game);
        } else if (className.equals(ClientLobbyScreen.class.getName())) {
            return getClientLobbyScreen(game);
        } else if (className.equals(MapStartPosScreen.class.getName())) {
            return getMapStartPosScreen(game);
        } else if (className.equals(GameScreen.class.getName())) {
            return getGameScreen(game);
        } else if (className.equals(VictoryScreen.class.getName())) {
            return getVictoryScreen(game);
        }

        // Generic instantiation
        return (Screen) ClassReflection.getConstructor(targetScreenClass, Zoned.class).newInstance(game);
    }

    private static Screen getServerLobbyScreen(Zoned game) throws ReflectionException {
        final Server server = new Server(HostJoinScreen.CONNECTION_BUFFER_SIZE, HostJoinScreen.SERVER_OBJECT_BUFFER_SIZE);

        Kryo kryo = server.getKryo();
        KryoHelper.registerClasses(kryo);

        server.start();
        try {
            server.bind(Constants.SERVER_PORT, Constants.SERVER_PORT);
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }

        return (Screen) ClassReflection.getConstructors(ServerLobbyScreen.class)[0].newInstance(game, server, "Boss #" + random.nextInt(100));
    }

    private static Screen getClientLobbyScreen(Zoned game) throws ReflectionException {
        final Client client = new Client(HostJoinScreen.CLIENT_WRITE_BUFFER_SIZE, HostJoinScreen.CONNECTION_BUFFER_SIZE);

        Kryo kryo = client.getKryo();
        KryoHelper.registerClasses(kryo);

        client.start();

        // Discover host blocks, but it's fine for testing purposes
        final InetAddress addr = client.discoverHost(Constants.SERVER_PORT, 4000);

        try {
            if (addr == null) {
                throw new IOException("Failed to find the host");
            }
            client.connect(4000, addr, Constants.SERVER_PORT, Constants.SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (!client.isConnected()) {
            Gdx.app.error(Constants.LOG_TAG, "Failed to connect for some reason");
            return null;
        }

        return (Screen) ClassReflection.getConstructors(ClientLobbyScreen.class)[0].newInstance(game, client, "Gamer #" + random.nextInt(100));
    }

    private static Screen getMapStartPosScreen(Zoned game) throws ReflectionException {
        // Loads the rectangle map with the specified extra dimensions
        MapManager mapManager = new MapManager();
        MapEntity map = mapManager.getMapList().first();
        MapExtraParams mapExtraParams = map.getExtraParams();
        mapExtraParams.extraParams = new int[] {5, 5};
        map.applyExtraParams();
        try {
            mapManager.prepareMap(map);
        } catch (InvalidMapCharacter | NoStartPositionsFound | InvalidMapDimensions e) {
            e.printStackTrace();
            return null;
        }

        Array<GridPoint2> startPositions = mapManager.getPreparedStartPositions();
        int playerCount = game.preferences.getInteger(Preferences.SPLITSCREEN_PLAYER_COUNT_PREFERENCE, 2);
        final Player[] players = new Player[playerCount];
        for (int i = 0; i < players.length; i++) {
            Color playerColor = PlayerColorHelper.getColorFromIndex(i % Constants.PLAYER_COLORS.size());
            players[i] = new Player(playerColor, PlayerColorHelper.getStringFromColor(playerColor));
            players[i].setControlIndex(i % Constants.PLAYER_CONTROLS.length);
            players[i].setStartPos(startPositions.get(i % startPositions.size));
        }

        int startPosSplitScreenCount = game.preferences.getInteger(Preferences.MAP_START_POS_SPLITSCREEN_COUNT_PREFERENCE, 2);
        return (Screen) ClassReflection.getConstructors(MapStartPosScreen.class)[0].newInstance(game, mapManager, players, startPosSplitScreenCount);
    }

    private static Screen getGameScreen(Zoned game) throws ReflectionException {
        // Loads the rectangle map with the specified extra dimensions
        MapManager mapManager = new MapManager();
        MapEntity map = mapManager.getMapList().first();
        MapExtraParams mapExtraParams = map.getExtraParams();
        mapExtraParams.extraParams = new int[] {5, 5};
        map.applyExtraParams();
        try {
            mapManager.prepareMap(map);
        } catch (InvalidMapCharacter | NoStartPositionsFound | InvalidMapDimensions e) {
            e.printStackTrace();
            return null;
        }

        Array<GridPoint2> startPositions = mapManager.getPreparedStartPositions();
        int playerCount = game.preferences.getInteger(Preferences.SPLITSCREEN_PLAYER_COUNT_PREFERENCE, 2);
        final Player[] players = new Player[playerCount];
        for (int i = 0; i < players.length; i++) {
            Color playerColor = PlayerColorHelper.getColorFromIndex(i % Constants.PLAYER_COLORS.size());
            players[i] = new Player(playerColor, PlayerColorHelper.getStringFromColor(playerColor));
            players[i].setControlIndex(i % Constants.PLAYER_CONTROLS.length);
            players[i].setStartPos(startPositions.get(i % startPositions.size));
        }

        return (Screen) ClassReflection.getConstructor(GameScreen.class, Zoned.class, MapManager.class, Player[].class).newInstance(game, mapManager, players);
    }

    private static Screen getVictoryScreen(Zoned game) throws ReflectionException {
        int total = 50;

        Array<TeamData> teamData = new Array<>();
        TeamData teamData1 = new TeamData(PlayerColorHelper.getColorFromIndex(0));
        teamData1.incrementScore(10);
        teamData1.setCapturePercentage(total);
        teamData.add(teamData1);
        TeamData teamData2 = new TeamData(PlayerColorHelper.getColorFromIndex(1));
        teamData2.incrementScore(40);
        teamData2.setCapturePercentage(total);
        teamData.add(teamData2);

        return (Screen) ClassReflection.getConstructors(VictoryScreen.class)[0].newInstance(game, teamData);
    }
}
