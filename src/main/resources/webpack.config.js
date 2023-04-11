var path = require('path');

module.exports = {
    entry: {
        'activity-monitor': './static/js/activity-monitor.js',
        common: './static/js/common.js',
        login: './static/js/login.js',
        model: './static/js/model.js',
        search: './static/js/search.js',
        profile: './static/js/profile.js',
        view: './static/js/view.js',
        ontology: './static/js/ontology.js',
        admin: './static/js/admin.js'
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
                test: /\.scss$/,
                use: ['style-loader', 'css-loader', 'postcss-loader', 'sass-loader'],
                exclude: /node_modules/
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
            }
        ]
    },
    devtool: 'source-map'
};