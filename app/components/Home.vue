<template>
    <Page>
        <ActionBar>
            <Label text="Home" />
        </ActionBar>

        <GridLayout>
            <MapComponent @mapClicked="onMapClick" />
        </GridLayout>
    </Page>
</template>

<script lang="ts">
import { Component, Prop } from 'vue-property-decorator';
import Vue from 'nativescript-vue';
import MapComponent from './MapComponent';
import ThreeDView from './3DView.vue';
import { HillshadeRasterTileLayer } from '@nativescript-community/ui-carto/layers/raster';
import { TileDataSource } from '@nativescript-community/ui-carto/datasources';

const TO_RAD = Math.PI / 180;
export function toRadians(value) {
    return value * TO_RAD;
}

function clipByRange(n, range) {
    return n % range;
}

function clip(n, minValue, maxValue) {
    return Math.min(Math.max(n, minValue), maxValue);
}
export function latLngToTileXY(lat, lng, zoom, tileSize = 256) {
    const MinLatitude = -85.05112878,
        MaxLatitude = 85.05112878,
        MinLongitude = -180,
        MaxLongitude = 180,
        mapSize = Math.pow(2, zoom) * tileSize;

    const latitude = clip(lat, MinLatitude, MaxLatitude);
    const longitude = clip(lng, MinLongitude, MaxLongitude);

    const p: { x?: number; y?: number } = {};
    p.x = ((longitude + 180.0) / 360.0) * (1 << zoom);
    p.y = ((1.0 - Math.log(Math.tan((latitude * Math.PI) / 180.0) + 1.0 / Math.cos(toRadians(lat))) / Math.PI) / 2.0) * (1 << zoom);

    const tilex = Math.trunc(p.x);
    const tiley = Math.trunc(p.y);
    const pixelX = Math.trunc(clipByRange((p.x - tilex) * tileSize, tileSize));
    const pixelY = Math.trunc(clipByRange((p.y - tiley) * tileSize, tileSize));

    const result = {
        x: tilex,
        y: tiley,
        pixelX,
        pixelY
    };
    return result;
}

@Component({
    components: {
        MapComponent
    }
})
export default class extends Vue {
    onMapClick(event) {
        try {
            const { clickType, position } = event.data;
            const hillshadeDataSource = event.hillshadeDataSource as TileDataSource<any, any>;

            const zoom = hillshadeDataSource.getNative().getMaxZoom();
            const tile = latLngToTileXY(position.latitude, position.longitude, zoom, 256);
            const tileData = hillshadeDataSource.loadTile(tile.x, tile.y, zoom);
            let altitude = 0;
            const data = tileData?.getData()?.getData();
            if (data) {
                if (global.isAndroid) {
                    const bmp = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.length);
                    const pixel = bmp.getPixel(tile.pixelX, tile.pixelY);
                    const R = (pixel >> 16) & 0xff;
                    const G = (pixel >> 8) & 0xff;
                    const B = pixel & 0xff;
                    altitude = Math.round(-32768 + (R * 256 + G + B / 256));
                    bmp.recycle();
                }
            }
            // if (data) {
            //     const image = new ImageAsset();
            //     image.loadFromBytes(data);
            //     const bytes = image.native.getBytes();
            //     let rgbOffset = 4;
            //     if (bytes.length === tileSize * tileSize * 3) {
            //         rgbOffset = 3;
            //     }
            //     console.log('bytes', bytes.length);
            //     const pixelIndex = tile.pixelY * tileSize + tile.pixelX;
            //     const R = data[rgbOffset * pixelIndex + 0] & 0xff;
            //     const G = data[rgbOffset * pixelIndex + 1] & 0xff;
            //     const B = data[rgbOffset * pixelIndex + 2] & 0xff;

            //     console.log(
            //         'pixelIndex',
            //         tile.pixelX,
            //         tile.pixelY,
            //         tileSize,
            //         pixelIndex,
            //         rgbOffset,
            //         data[rgbOffset * pixelIndex + 0],
            //         data[rgbOffset * pixelIndex + 1],
            //         data[rgbOffset * pixelIndex + 2],
            //         R,
            //         G,
            //         B
            //     );
            //     altitude = -32768 + R * 256 + G + B / 256;
            // }

            this.$navigateTo(ThreeDView, {
                animated: true,
                props: {
                    position: { ...position, altitude },
                    dataSource: hillshadeDataSource
                }
            });
        } catch (err) {
            console.error(err, err.stack);
        }
    }
}
</script>

<style scoped lang="scss">
@import '@nativescript/theme/scss/variables/blue';

// Custom styles
.fas {
    @include colorize($color: accent);
}

.info {
    font-size: 20;
    horizontal-align: center;
    vertical-align: center;
}
</style>
