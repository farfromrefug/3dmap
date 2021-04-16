import { TileDataSource } from '@nativescript-community/ui-carto/datasources';
import { PersistentCacheTileDataSource } from '@nativescript-community/ui-carto/datasources/cache';
import { HTTPTileDataSource } from '@nativescript-community/ui-carto/datasources/http';
import { HillshadeRasterTileLayer, HillshadeRasterTileLayerOptions, RasterTileFilterMode, RasterTileLayer } from '@nativescript-community/ui-carto/layers/raster';
import { Projection } from '@nativescript-community/ui-carto/projections';
// import { MapBoxElevationDataDecoder, TerrariumElevationDataDecoder } from '@nativescript-community/ui-carto/rastertiles';
import { CartoMap } from '@nativescript-community/ui-carto/ui';
import { Color } from '@nativescript/core';
import { File, Folder, knownFolders, path } from '@nativescript/core/file-system';
import Vue from 'nativescript-vue';
import { Component, Prop } from 'vue-property-decorator';

@Component({})
export default class MapComponent extends Vue {
    _cartoMap: CartoMap<LatLonKeys> = null;
    mapProjection: Projection = null;
    rasterLayer: RasterTileLayer = null;
    hillshadeDataSource: TileDataSource<any, any> = null;
    hillshadeLayer: HillshadeRasterTileLayer = null;

    // @Prop() session: Session;
    // @Prop({ default: false }) readonly licenseRegistered!: boolean;
    @Prop({ default: 13 }) readonly zoom!: number;
    @Prop({ default: 1 }) readonly layerOpacity!: number;
    @Prop() readonly vectorTileClicked!: Function;

    get cartoMap() {
        return this._cartoMap;
    }

    createHillshadeTileLayer(dataSource, options: HillshadeRasterTileLayerOptions = {}, terrarium = false) {
        const contrast = 1;
        const heightScale = 0.086;
        const illuminationDirection = 335;
        const opacity = 1;
        // const decoder = terrarium ? new TerrariumElevationDataDecoder() : new MapBoxElevationDataDecoder();
        const tileFilterModeStr = 'bilinear' as any;

        const accentColor = new Color('rgba(0,0,0,0.39)');
        const shadowColor = new Color('rgba(0,0,0,0.39)');
        const highlightColor = new Color('rgba(255, 255, 255,0)');

        let tileFilterMode: RasterTileFilterMode;
        switch (tileFilterModeStr) {
            case 'bicubic':
                tileFilterMode = RasterTileFilterMode.RASTER_TILE_FILTER_MODE_BICUBIC;
                break;
            case 'bilinear':
                tileFilterMode = RasterTileFilterMode.RASTER_TILE_FILTER_MODE_BILINEAR;
                break;
            case 'nearest':
                tileFilterMode = RasterTileFilterMode.RASTER_TILE_FILTER_MODE_NEAREST;
                break;
        }
        return new HillshadeRasterTileLayer({
            // decoder,
            tileFilterMode,
            visibleZoomRange: [3, 16],
            contrast,
            // exagerateHeightScaleEnabled: false,
            // illuminationDirection: [Math.sin(toRadians(illuminationDirection)), Math.cos(toRadians(illuminationDirection)), 0] as any,
            highlightColor,
            // accentColor,
            heightScale,
            dataSource,
            opacity,
            ...options
        });
    }
    onMapReady(e) {
        const cartoMap = (this._cartoMap = e.object as CartoMap<LatLonKeys>);

        this.mapProjection = cartoMap.projection;

        const options = cartoMap.getOptions();
        options.setWatermarkScale(0);
        // options.setWatermarkPadding(toNativeScreenPos({ x: 80, y: 0 }));
        options.setRestrictedPanning(true);
        options.setSeamlessPanning(true);
        options.setEnvelopeThreadPoolSize(2);
        options.setTileThreadPoolSize(2);
        options.setZoomGestures(true);
        options.setRotatable(false);

        cartoMap.setZoom(this.zoom, 0);
        cartoMap.setFocusPos({ latitude: 45.171547, longitude: 5.722387 }, 0);

        // options.setDrawDistance(8);
        // if (appSettings.getString('mapFocusPos')) {
        //     console.log('saved focusPos', appSettings.getString('mapFocusPos'));
        //     cartoMap.setFocusPos(JSON.parse(appSettings.getString('mapFocusPos')), 0);
        // }

        const cacheFolder = Folder.fromPath(path.join(knownFolders.documents().path, 'carto_cache'));
        const dataSource = new PersistentCacheTileDataSource({
            dataSource: new HTTPTileDataSource({
                minZoom: 1,
                subdomains: 'abc',
                maxZoom: 20,
                url: 'https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png'
            }),
            capacity: 300 * 1024 * 1024,
            databasePath: path.join(cacheFolder.path, 'cache.db')
        });
        this.rasterLayer = new RasterTileLayer({
            zoomLevelBias: 1,
            opacity: this.layerOpacity,
            dataSource
        });
        cartoMap.addLayer(this.rasterLayer);
        this.hillshadeDataSource = new PersistentCacheTileDataSource({
            dataSource: new HTTPTileDataSource({
                url: 'https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png',
                minZoom: 5,
                maxZoom: 11
            }),
            capacity: 300 * 1024 * 1024,
            // minZoom: 5,
            // maxZoom: 11,
            databasePath: path.join(cacheFolder.path, 'hillshade.db')
        });
        this.hillshadeLayer = this.createHillshadeTileLayer(this.hillshadeDataSource);
        // cartoMap.addLayer(this.hillshadeLayer);

        // this.getOrCreateLocalVectorTileLayer();
        // this.ignoreStable = true;
        // this.localVectorTileDataSource.setLayerGeoJSON(1, perimeterGeoJSON);

        console.log('onMapReady', this.zoom, cartoMap.zoom, cartoMap.focusPos, 0);
        // setTimeout(() => {
        // perms
        // .request('storage')
        // .then(status => {
        // console.log('on request storage', status, this.actionBarButtonHeight, !!this._cartoMap);
        // if (status === 'authorized') {
        // this.$packageService.start();
        // this.setMapStyle(appSettings.getString('mapStyle', 'alpimaps.zip'));
        // this.runOnModules('onMapReady', this, cartoMap);
        // cartoMap.requestRedraw();
        // console.log('onMapReady', 'done');
        // } else {
        //     return Promise.reject(status);
        // }
        // })
        // .catch(err => console.error(err));
        // }, 0);
        // this.updateSession();
        // if (this.sessionLine) {
        //     const zoomBounds = this.sessionLine.getBounds();
        //     const zoomLevel = getBoundsZoomLevel(zoomBounds, { width: screen.mainScreen.widthDIPs, height: screen.mainScreen.heightDIPs });
        //     cartoMap.setZoom(Math.min(zoomLevel, 18), 0);
        //     cartoMap.setFocusPos(getCenter(zoomBounds.northeast, zoomBounds.southwest), 0);
        // }
        this.$emit('mapReady', e);
    }
    onMapMove(e) {
        // console.log('onMapMove',this._cartoMap.zoom, this._cartoMap.focusPos);
        this.$emit('mapMove', e);
    }
    onMapStable(e) {
        // this.log('onMapStable', this.ignoreStable);

        this.$emit('mapStable', e);
    }
    onMapIdle(e) {
        this.$emit('mapIdle', e);
    }
    onMapClicked(e) {
        this.$emit('mapClicked', { ...e, hillshadeLayer: this.hillshadeLayer, hillshadeDataSource: this.hillshadeDataSource });
    }
}
