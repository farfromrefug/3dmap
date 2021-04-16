<template>
    <Page @closingModally="onNavigatingFrom" @navigatingFrom="onNavigatingFrom" @navigatingTo="onNavigatingTo">
        <ActionBar>
            <Label text="3D" />
        </ActionBar>

        <GridLayout>
            <Canvas @ready="canvasReady" backgroundColor="black" />
        </GridLayout>
    </Page>
</template>

<script lang="ts">
import { Component, Prop } from 'vue-property-decorator';
import Vue from 'nativescript-vue';
import { Deck } from '@deck.gl/core';
import { FirstPersonView } from '@deck.gl/core';
import TerrainLayer from '~/deckgl/TerrainLayer';
import { HTTPTileDataSource } from '@nativescript-community/ui-carto/datasources/http';
import { ScatterplotLayer } from '@deck.gl/layers';
import { TileDataSource } from '@nativescript-community/ui-carto/datasources';

if (global.isAndroid) {
    java.lang.System.loadLibrary('canvasnative');
}

@Component({})
export default class extends Vue {
    @Prop() position;
    @Prop() dataSource: TileDataSource<any, any>;
    canvas;
    deckgl: Deck;
    terrainLayer;
    async canvasReady(args) {
        this.canvas = args.object;
        // WebGL2RenderingContext.isDebug = true;
        // WebGL2RenderingContext.filter = 'both';
        // WebGLRenderingContext.isDebug = true;
        // WebGLRenderingContext.filter = 'both';
        // context.isDebug = true;
        try {
            const context = this.canvas.getContext('webgl2');
            const INITIAL_VIEW_STATE = {
                latitude: this.position.latitude,
                longitude: this.position.longitude,
                zoom: 13.5,
                maxZoom: 20,
                pitch: 0,
                position: [0, 0, this.position.altitude + 300],
                maxPitch: 90,
                bearing: 0
            };
            const nDataSource = this.dataSource.getNative();
            this.terrainLayer = new TerrainLayer({
                id: 'terrain',
                refinementStrategy: 'never',
                meshMaxError: 10,
                maxRequests: 1,
                minZoom: nDataSource.getMinZoom(),
                maxZoom: nDataSource.getMaxZoom(),
                // minZoom: 5,
                // maxZoom: 11,
                dataSource: nDataSource,
                elevationDecoder: {
                    rScaler: 256,
                    gScaler: 1,
                    bScaler: 1 / 256,
                    offset: -32768
                }
            });

            const pixelRatio = 0.2;

            const { drawingBufferWidth: width, drawingBufferHeight: height } = context;

            const mainView = new FirstPersonView({
                id: 'mainView',
                near: 10,
                far: 60000,
                focalDistance: 1000
            });
            this.deckgl = new Deck({
                gl: context,
                views: [mainView],
                onError: (error: Error, source: any) => {
                    console.log('onError', error);
                },
                width,
                height,
                useDevicePixels: pixelRatio,
                initialViewState: INITIAL_VIEW_STATE,
                controller: { dragMode: 'rotate' },
                layers: [this.terrainLayer]
                // onWebGLInitialized: (gl: WebGLRenderingContext) => console.log('onWebGLInitialized'),
                // onViewStateChange: (args: {
                //     viewState: any;
                //     interactionState: {
                //         inTransition?: boolean;
                //         isDragging?: boolean;
                //         isPanning?: boolean;
                //         isRotating?: boolean;
                //         isZooming?: boolean;
                //     };
                //     oldViewState: any;
                // }) => console.log('onViewStateChange', args),
                // onHover: <D>(info, e: MouseEvent) => console.log('onHover'),
                // onClick: <D>(info, e: MouseEvent) => console.log('onClick'),
                // onDragStart: <D>(info, e: MouseEvent) => console.log('onDragStart'),
                // onDrag: <D>(info, e: MouseEvent) => console.log('onDrag'),
                // onDragEnd: <D>(info, e: MouseEvent) => console.log('onDragEnd'),
                // onLoad: () => console.log('onLoad'),
                // onResize: (size: { height: number; width: number }) => console.log('onResize'),
                // onBeforeRender: (args: { gl: WebGLRenderingContext }) => console.log('onBeforeRender'),
                // onAfterRender: (args: { gl: WebGLRenderingContext }) => console.log('onAfterRender')
            });
            console.log('canvasReady', !!this.canvas, !!context);
        } catch (err) {
            console.error(err, err.stack);
        }
    }
    onNavigatingTo(e) {
        // console.log('onNavigatingTo', page && page.nativeView, e.object);
    }

    onNavigatingFrom(e) {
        console.log('onNavigatingFrom', !!this.deckgl);
        if (this.deckgl) {
            this.deckgl.finalize();
            this.deckgl = null;
        }
        // console.log('onNavigatingTo', page && page.nativeView, e.object);
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
