package org.nativescript.testapp;

import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// import org.nativescript.canvas.TNSImageAsset;
import com.github.triniwiz.canvas.TNSImageAsset;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.carto.core.MapTile;
import com.carto.core.BinaryData;
import com.carto.datasources.TileDataSource;
import com.carto.datasources.components.TileData;

public final class MartiniMesh {
    private static final Handler handler = new Handler(Looper.getMainLooper());
    static HashMap<Number, Martini> martinis = new HashMap();
    static ExecutorService executorService = Executors.newFixedThreadPool(4);


    public interface Callback {
        void onSuccess(@NonNull ByteBuffer triangles, @NonNull ByteBuffer positions, @NonNull ByteBuffer textCoords, @Nullable float[][] boundingBox);

        void onError(@Nullable Throwable var1);
    }


    @NonNull
    public static float[] getTerrain(@NonNull byte[] imageData, int tileSize, @NonNull JSONObject elevationDecoder) throws JSONException {
        float rScaler = (float)elevationDecoder.getDouble("rScaler");
        float bScaler = (float)elevationDecoder.getDouble("bScaler");
        float gScaler = (float)elevationDecoder.getDouble("gScaler");
        float offset = (float)elevationDecoder.getDouble("offset");

        int rgbOffset = 4;
        if (imageData.length == tileSize * tileSize * 3) {
            rgbOffset = 3;
        }
        // Log.d("JS", "getTerrain " + imageData.length + " " + tileSize + " " + rgbOffset);

        int gridSize = tileSize + 1;
        float[] terrain = new float[gridSize * gridSize];
        int k = 0;
        int r = 0;
        int g = 0;
        int b = 0;

        for (int y = 0; y < tileSize; y++) {
            for ( int x = 0; x < tileSize; x++) {
                k =  (y * tileSize + x) * rgbOffset;
                r = ((int) imageData[k + 0] & 0xff);
                g = (((int) imageData[k + 1] & 0xff) );
                b = (((int) imageData[k + 2] & 0xff) );
                terrain[y * gridSize + x] = r * rScaler +  g * gScaler + b * bScaler + offset;
            }
        }

        // backfill right and bottom borders
        for (int x = 0; x < gridSize - 1; x++) {
            terrain[gridSize * (gridSize - 1) + x] = terrain[gridSize * (gridSize - 2) + x];
        }
        for (int y = 0; y < gridSize; y++) {
            terrain[gridSize * y + gridSize - 1] = terrain[gridSize * y + gridSize - 2];
        }

        // backfill bottom border
        // for ( i = gridSize * (gridSize - 1), x = 0; x < gridSize - 1; x++, i++) {
        //     terrain[i] = terrain[i - gridSize];
        // }
        // // backfill right border
        // for ( i = gridSize - 1, y = 0; y < gridSize; y++, i += gridSize) {
        //     terrain[i] = terrain[i - 1];
        // }

        return terrain;
    }

    @NonNull
    public static Pair getMeshAttributes(@NonNull int[] vertices, @NonNull float[] terrain, float tileSize, @NonNull JSONArray bounds) throws JSONException {
        float gridSize = tileSize + 1;
        int numOfVerticies = vertices.length / 2;
        float[] positions = new float[numOfVerticies * 3];
        float[] texCoords = new float[numOfVerticies * 2];
        float minX = 0;
        float minY = 0;
        float maxX = tileSize;
        float maxY = tileSize;
        if (bounds != null ) {
            minX = (float) bounds.getDouble(0);
            minY = (float) bounds.getDouble(1);
            maxX = (float) bounds.getDouble(2);
            maxY = (float) bounds.getDouble(3);
        }
        float xScale = (float) (maxX - minX) / tileSize;
        float yScale = (float) (maxY - minY) / tileSize;

        for (int i = 0; i < numOfVerticies; i++) {
            int x = vertices[i * 2];
            int y = vertices[i * 2 + 1];
            int pixelIdx = y * (int) gridSize +  x;
            positions[3 * i + 0] = x * xScale + minX;
            positions[3 * i + 1] = (-y) * yScale + maxY;
            positions[3 * i + 2] = terrain[pixelIdx];

            texCoords[2 * i + 0] = x / tileSize;
            texCoords[2 * i + 1] = y / tileSize;
        }

        return new Pair(positions, texCoords);
    }

    @Nullable
    public static float[][] getMeshBoundingBox(@NonNull float[] positions) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;
        int len = positions.length;
        if (len == 0) {
            return null;
        } else {

            for (int i = 0; i < len; i += 3) {

                float x = positions[i];
                float y = positions[i + 1];
                float z = positions[i + 2];
                minX = x < minX ? x : minX;
                minY = y < minY ? y : minY;
                minZ = z < minZ ? z : minZ;
                maxX = x > maxX ? x : maxX;
                maxY = y > maxY ? y : maxY;
                maxZ = z > maxZ ? z : maxZ;
            }
            return (float[][]) (new float[][]{{minX, minY, minZ}, {maxX, maxY, maxZ}});
        }
    }

    public static void getMartiniTileMesh(String tile, TileDataSource dataSource, @NonNull final String terrainOptionsStr, @NonNull final MartiniMesh.Callback callback) {
        Thread thread = new Thread(new Runnable() {
    @Override
    public void run() {
        try {
            long startTime = System.currentTimeMillis();
            JSONObject tileJSON = new JSONObject(tile);
            int z = tileJSON.getInt("z");
            int x = tileJSON.getInt("x");
            int y = tileJSON.getInt("y");
            // Log.d("JS", "getMartiniTileMesh " + x + " " + y + " " + z);
            TileData tileData = dataSource.loadTile(new MapTile(x,y, z, 0));
            Log.d("JS", "getMartiniTileMesh loadTile " + (System.currentTimeMillis() - startTime) + " ms");
            if (tileData == null) {
                MartiniMesh.handler.post((Runnable) (new Runnable() {
                    public final void run() {
                        callback.onError((Exception) null);
                    }
                }));
                return;
            }
            BinaryData binaryData = tileData.getData();
            if (binaryData == null) {
                MartiniMesh.handler.post((Runnable) (new Runnable() {
                    public final void run() {
                        callback.onError((Exception) null);
                    }
                }));
                return;
            }
            Log.d("JS", "getMartiniTileMesh getTileData " + (System.currentTimeMillis() - startTime) + " ms");
            TNSImageAsset asset = new TNSImageAsset();
            byte[] binaryDataData = binaryData.getData();
            Log.d("JS", "getMartiniTileMesh getTileDataData " + (System.currentTimeMillis() - startTime) + " ms");
            asset.loadImageFromBytes(binaryDataData);
            Log.d("JS", "getMartiniTileMesh loadImageFromBytes " + (System.currentTimeMillis() - startTime) + " ms");
            byte[] data = asset.getBytes();
            int width = asset.getWidth();
            if (data == null) {
                // MartiniMesh.handler.post((Runnable) (new Runnable() {
                    // public final void run() {
                        callback.onError((Exception) null);
                //     }
                // }));
                return;
            } else {
                JSONObject terrainOptions = new JSONObject(terrainOptionsStr);

                int meshMaxError = terrainOptions.getInt("meshMaxError");
                JSONArray bounds = terrainOptions.getJSONArray("bounds");
                JSONObject elevationDecoder = terrainOptions.getJSONObject("elevationDecoder");
                int gridSize = width + 1;

                try {
                    getMartiniTileMesh(tile, data, width, terrainOptionsStr, callback);
                } catch (final Throwable exception) {
                    exception.printStackTrace();
                    // handler.post((Runnable) (new Runnable() {
                        // public final void run() {
                            callback.onError(exception);
                    //     }
                    // }));
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
            // handler.post((Runnable) (new Runnable() {
                // public final void run() {
                    callback.onError(e);
            //     }
            // }));
        }
    }
  });
  thread.start();
    }
    public static void getMartiniTileMesh(String tileStr, byte[]data, int width, @NonNull final String terrainOptionsStr, @NonNull final MartiniMesh.Callback callback) throws JSONException {
       
        long startTime = System.currentTimeMillis();
                    
        JSONObject terrainOptions = new JSONObject(terrainOptionsStr);

        int meshMaxError = terrainOptions.getInt("meshMaxError");
        JSONObject elevationDecoder = terrainOptions.getJSONObject("elevationDecoder");
        int gridSize = width + 1;

        try {
            float[] terrain = getTerrain(data, width, elevationDecoder);
            Log.d("JS", "getMartiniTileMesh2 getTerrain " + (System.currentTimeMillis() - startTime) + " ms");
            // Log.d("JS", "terrain " + tileStr + " " + terrain.length + " " + Arrays.toString(terrain));
            Martini martini = martinis.get(gridSize);
            if (martini == null) {
                martini = new Martini(gridSize);
                martinis.put(gridSize, martini);
            }
            Martini.Tile tile = martini.createTile(terrain);
            Log.d("JS", "getMartiniTileMesh2 createTile " + (System.currentTimeMillis() - startTime) + " ms");
            Pair<int[], int[]> result = tile.getMesh(meshMaxError);
            int[] vertices = result.first;
            final int[] triangles = result.second;
            // Log.d("JS", "vertices " + tileStr + " " + vertices.length + " " + Arrays.toString(vertices));
            // Log.d("JS", "triangles " + tileStr + " " + triangles.length + " " + Arrays.toString(triangles));
            Log.d("JS", "getMartiniTileMesh2 getMesh " + (System.currentTimeMillis() - startTime) + " ms");

            JSONArray bounds = null;
            if (terrainOptions.has("bounds")) {
                bounds = terrainOptions.getJSONArray("bounds");
            }
            Pair attributes = getMeshAttributes(vertices, terrain, width, bounds);
            Log.d("JS", "getMartiniTileMesh2 getMeshAttributes " + (System.currentTimeMillis() - startTime) + " ms");
            final float[] positions = (float[]) attributes.first;
            final float[] texCoords = (float[]) attributes.second;
            final float[][] boundingBox = MartiniMesh.getMeshBoundingBox(positions);
            // Log.d("JS", "positions " + tileStr + " " + positions.length + " " + Arrays.toString(positions));
            Log.d("JS", "getMartiniTileMesh2 getMeshBoundingBox " + (System.currentTimeMillis() - startTime) + " ms");

            final ByteBuffer trianglesBuffer = ByteBuffer.allocateDirect((int)(triangles.length * 4)).order(ByteOrder.nativeOrder());
            final ByteBuffer positionsBuffer = ByteBuffer.allocateDirect((int)(positions.length * 4)).order(ByteOrder.nativeOrder());
            final ByteBuffer texCoordsBuffer = ByteBuffer.allocateDirect((int)(texCoords.length * 4)).order(ByteOrder.nativeOrder());
            trianglesBuffer.asIntBuffer().put(triangles);
            positionsBuffer.asFloatBuffer().put(positions);
            texCoordsBuffer.asFloatBuffer().put(texCoords);
            Log.d("JS", "getMartiniTileMesh2 ByteBuffers " + (System.currentTimeMillis() - startTime) + " ms");

            // handler.post((Runnable) (new Runnable() {
                // public final void run() {
                    callback.onSuccess(trianglesBuffer, positionsBuffer, texCoordsBuffer, boundingBox);
                // }
            // }));
        } catch (final Throwable exception) {
            exception.printStackTrace();
            // handler.post((Runnable) (new Runnable() {
                // public final void run() {
                    callback.onError(exception);
                // }
            // }));
        }
    }

}
