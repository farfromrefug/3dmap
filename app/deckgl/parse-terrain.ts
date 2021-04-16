export default async function loadTerrain({ tile, dataSource }, options, context?) {
    return new Promise(async (resolve, reject) => {
        console.log('loadTerrain', { x: tile.x, y: tile.y, z: tile.z }, options.myterrain);
        (org.nativescript as any).testapp.MartiniMesh.getMartiniTileMesh(
            JSON.stringify({ x: tile.x, y: tile.y, z: tile.z }),
            dataSource,
            JSON.stringify(options.myterrain),
            new (org.nativescript as any).testapp.MartiniMesh.Callback({
                onError(error) {
                    console.error('onError', error, error && Object.keys(error));
                    reject(error);
                },
                onSuccess(triangles: any[], positions: any[], texCoords: any[], boundingBox: number[][]) {
                    const start = Date.now();
                    const trianglesData = new Uint32Array((ArrayBuffer as any).from(triangles));
                    const positionsData = new Float32Array((ArrayBuffer as any).from(positions));
                    const texCoordsData = new Float32Array((ArrayBuffer as any).from(texCoords));
                    console.log('onSuccess', tile.x, tile.y, tile.z, Date.now() - start, 'ms');
                    const bbox = [];
                    for (let i = 0; i < boundingBox.length; i++) {
                        const subArray = boundingBox[i];
                        const sub = [];
                        for (let j = 0; j < subArray.length; j++) {
                            sub[j] = subArray[j];
                        }
                        bbox[i] = sub;
                    }
                    resolve({
                        // Data return by this loader implementation
                        loaderData: {
                            header: {}
                        },
                        header: {
                            vertexCount: trianglesData.length,
                            boundingBox: bbox
                        },
                        mode: 4, // TRIANGLES
                        indices: { value: trianglesData, size: 1 },
                        attributes: {
                            POSITION: { value: positionsData, size: 3 },
                            TEXCOORD_0: { value: texCoordsData, size: 2 }
                            // NORMAL: {}, - optional, but creates the high poly look with lighting
                        }
                    });
                }
            })
        );
    });

    // const image = new ImageAsset();
    // await image.loadFromBytesAsync(new Uint8Array(arrayBuffer));
    // const result = await createImageBitmap(image as any);
    // //@ts-ignore
    // const imageData = new ImageData(result.native);
    // console.log('loadTerrain', result, options.myterrain);
    // return getMartiniTileMesh(imageData, options.myterrain);
}
