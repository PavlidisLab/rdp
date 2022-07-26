var path = require('path');

module.exports = {
    mode: 'production',
    entry: {
        'activity-monitor': './static/js/activity-monitor.js',
        common: './static/js/common.js',
        login: './static/js/login.js',
        model: './static/js/model.js',
        search: './static/js/search.js',
        profile: './static/js/profile.js',
        view: './static/js/view.js',
        ontology: './static/js/ontology.js'
    },
    output: {
        filename: '[name].bundle.js',
        path: path.resolve(__dirname, 'static/dist')
    },
    module: {
        rules: [
            {
                test: /\.css$/,
                use: ['style-loader', 'css-loader']
            },
            {
                test: /\.js$/,
                exclude: /node_modules/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        presets: ['@babel/preset-env']
                    }
                }
            },
            {
                test: /\.js$/,
                enforce: 'pre',
                use: ['source-map-loader'],
            },
        ]
    }
};