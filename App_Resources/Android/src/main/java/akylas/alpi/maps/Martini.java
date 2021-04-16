package org.nativescript.testapp;

import android.util.Pair;

import java.util.Arrays;

import androidx.annotation.NonNull;

public class Martini {
    private int gridSize = 257;
    private int numTriangles;
    private int numParentTriangles;
    // @NonNull
    // private int[] indices;
    @NonNull
    private int[] coords;

    @NonNull
    public final Martini.Tile createTile(@NonNull float[] terrain) throws Throwable {
        return new Martini.Tile(terrain);
    }

    public Martini(int gridSize) throws Throwable {
        this.gridSize = gridSize;
        int tileSize = gridSize - 1;
        if ((tileSize & tileSize - 1) == 1) {
            throw (Throwable) (new Exception("Expected grid size to be 2^n+1, got " + gridSize));
        } else {
            this.numTriangles = tileSize * tileSize * 2 - 2;
            this.numParentTriangles = numTriangles - tileSize * tileSize;
            // indices = new int[gridSize * gridSize];
            coords = new int[numTriangles * 4];
            int id = 0;
            int ax = 0;
            int ay = 0;
            int bx = 0;
            int by = 0;
            int cx = 0;
            int cy = 0;
            for (int i = 0; i < numTriangles; i++) {
                id = i + 2;
                ax = 0;
                ay = 0;
                bx = 0;
                by = 0;
                cx = 0;
                cy = 0;
                if ((id & 1) == 1) {
                    bx = tileSize;
                    by = tileSize;
                    cx = tileSize;
                } else {
                    ax = tileSize;
                    ay = tileSize;
                    cy = tileSize;
                }

                int mx;
                for (id >>= 1; id > 1; id >>= 1) {
                    mx = ax + bx >> 1;
                    int my = ay + by >> 1;
                    if ((id & 1) == 1) {
                        bx = ax;
                        by = ay;
                        ax = cx;
                        ay = cy;
                    } else {
                        ax = bx;
                        ay = by;
                        bx = cx;
                        by = cy;
                    }

                    cx = mx;
                    cy = my;
                }

                mx = i * 4;
                this.coords[mx + 0] = ax;
                this.coords[mx + 1] = ay;
                this.coords[mx + 2] = bx;
                this.coords[mx + 3] = by;
            }
        }

    }

    class Tile {
        @NonNull
        private final float[] terrain;
        @NonNull
        private final float[] errors;
        private int meshNumTriangles;
        private int meshNumVertices;
        private int[] indices;
        private int[] vertices;
        private int[] triangles;
        private int triIndex;
        private int maxError;

        public Tile(@NonNull float[] terrain) throws Throwable {
            super();
            this.terrain = terrain;
            indices = new int[gridSize * gridSize];
            if (terrain.length != gridSize * gridSize) {
                throw (Throwable) (new Exception("Expected terrain data of length " + gridSize * gridSize + " (" + gridSize + " x " + gridSize + "), got " + terrain.length + '.'));
            } else {
                this.errors = new float[terrain.length];
                this.update();
            }
        }

        public final void update() {
            int k, ax, ay, bx, by, mx, my, cx, cy, middleIndex, leftChildIndex, rightChildIndex;
            float interpolatedHeight, middleError;
            for (int i = numTriangles - 1; i >= 0; --i) {
                k = i * 4;
                ax = coords[k + 0];
                ay = coords[k + 1];
                bx = coords[k + 2];
                by = coords[k + 3];
                mx = ax + bx >> 1;
                my = ay + by >> 1;
                cx = mx + my - ay;
                cy = my + ax - mx;
                interpolatedHeight = (terrain[ay * gridSize + ax] + terrain[by * gridSize + bx]) / (float) 2;
                middleIndex = my * gridSize + mx;
                middleError = Math.abs(interpolatedHeight - terrain[middleIndex]);
                errors[middleIndex] = Math.max(errors[middleIndex], middleError);
                if (i < numParentTriangles) {
                    leftChildIndex = (ay + cy >> 1) * gridSize + (ax + cx >> 1);
                    rightChildIndex = (by + cy >> 1) * gridSize + (bx + cx >> 1);
                    errors[middleIndex] = Math.max(errors[middleIndex], Math.max(errors[leftChildIndex], errors[rightChildIndex]));
                }
            }

        }

        public final void processTriangle(int ax, int ay, int bx, int by, int cx, int cy) {
            int mx = ax + bx >> 1;
            int my = ay + by >> 1;
            if (Math.abs(ax - cx) + Math.abs(ay - cy) > 1 && errors[my * gridSize + mx] > (float) maxError) {
                processTriangle(cx, cy, ax, ay, mx, my);
                processTriangle(bx, by, cx, cy, mx, my);
            } else {
                int a = indices[ay * gridSize + ax] - 1;
                int b = indices[by * gridSize + bx] - 1;
                int c = indices[cy * gridSize + cx] - 1;
                vertices[2 * a] = ax;
                vertices[2 * a + 1] = ay;
                vertices[2 * b] = bx;
                vertices[2 * b + 1] = by;
                vertices[2 * c] = cx;
                vertices[2 * c + 1] = cy;
                triangles[triIndex++] = a;
                triangles[triIndex++] = b;
                triangles[triIndex++] = c;
            }

        }

        public final void countElements(int ax, int ay, int bx, int by, int cx, int cy) {
            int mx = ax + bx >> 1;
            int my = ay + by >> 1;
            if (Math.abs(ax - cx) + Math.abs(ay - cy) > 1 && errors[my * gridSize + mx] > maxError) {
                countElements(cx, cy, ax, ay, mx, my);
                countElements(bx, by, cx, cy, mx, my);
            } else {
                if (indices[ay * gridSize + ax] == 0) {
                    indices[ay * gridSize + ax] = ++meshNumVertices;
                }
                if (indices[by * gridSize + bx] == 0) {
                    indices[by * gridSize + bx] = ++meshNumVertices;
                }
                if (indices[cy * gridSize + cx] == 0) {
                    indices[cy * gridSize + cx] = ++meshNumVertices;
                }
                meshNumTriangles++;
            }

        }

        @NonNull
        public final Pair<int[], int[]> getMesh(int maxError) {
            maxError = maxError;
            meshNumVertices = 0;
            meshNumTriangles = 0;
            int max = gridSize - 1;

            countElements(0, 0, max, max, max, 0);
            countElements(max, max, 0, 0, 0, max);
            vertices = new int[meshNumVertices * 2];
            triangles = new int[meshNumTriangles * 3];
            triIndex = 0;
            processTriangle(0, 0, max, max, max, 0);
            processTriangle(max, max, 0, 0, 0, max);
            return new Pair(vertices, triangles);
        }
    }
}

