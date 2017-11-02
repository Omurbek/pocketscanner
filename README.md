## Requirements

Since the app uses Google's Map API and Microsoft's Translator Text API, you need to plug your own API keys for these services to use it. This is done by adding string resources either in the `strings.xml` file or a new `keys.xml` file in the `values` directory. String resource names should be `google_maps_api_key` and `microsoft_translator_api_key`.