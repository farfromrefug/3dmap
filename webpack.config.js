const webpack = require('@nativescript/webpack');
const { dirname, join, relative, resolve } = require('path');

module.exports = (env) => {
    webpack.init(env);

    const { fork = true } = env;

    // Learn how to customize:
    // https://docs.nativescript.org/webpack
    webpack.chainWebpack((config) => {
        const coreModulesPackageName = fork ? '@akylas/nativescript' : '@nativescript/core';
        config.resolve.modules
            .clear()
            .add(resolve(__dirname, `node_modules/${coreModulesPackageName}`))
            .add(resolve(__dirname, 'node_modules'))
            .add(`node_modules/${coreModulesPackageName}`)
            .add('node_modules');
        config.resolve.alias
            .set('@nativescript/core', `${coreModulesPackageName}`)
            .set('tns-core-modules', `${coreModulesPackageName}`)
            .set('mjolnir.js', '~/deckgl/EventManager')
            .set('./controller', '~/deckgl/Controller');
        config.externals(['fs', 'path', 'child_process', 'asciify-image', 'util', 'module']);

    });
    return webpack.resolveConfig();
};
