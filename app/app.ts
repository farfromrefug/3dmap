import Vue from 'nativescript-vue'
import Home from './components/Home.vue'

import CanvasPlugin from  '@nativescript/canvas/vue'
import CartoPlugin from '@nativescript-community/ui-carto/vue';
require('@nativescript/canvas-polyfill');
Vue.use(CanvasPlugin);
Vue.use(CartoPlugin);
declare let __DEV__: boolean;

// Prints Vue logs when --env.production is *NOT* set while building
Vue.config.silent = !__DEV__

new Vue({
  render: (h) => h('frame', [h(Home)]),
}).$start()
