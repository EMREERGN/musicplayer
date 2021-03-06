package app.sonu.com.musicplayer.mediaplayernew.musicsource;

import android.support.v4.media.MediaMetadataCompat;

import java.util.Iterator;

/**
 * Created by sonu on 27/7/17.
 * this interface represents a music provider source
 * if a class wants to be a music source, it must implement this interface
 * @author amanshu
 */

public interface MusicProviderSource {
    String CUSTOM_METADATA_KEY_TRACK_SOURCE = "__SOURCE__";
    String CUSTOM_METADATA_KEY_DATE_MODIFIED = "__DATA_ADDED__";
    String CUSTOM_METADATA_KEY_DISPLAY_NAME = "__DISPLAY_NAME__";
    String CUSTOM_METADATA_KEY_SIZE = "__SIZE__";
    String CUSTOM_METADATA_KEY_ALBUM_ID = "__ALBUM_ID__";
    String CUSTOM_METADATA_KEY_ALBUM_KEY = "__ALBUM_KEY__";
    String CUSTOM_METADATA_KEY_ARTIST_ID = "__ARTIST_ID__";
    String CUSTOM_METADATA_KEY_ARTIST_KEY = "__ARTIST_KEY__";
    String CUSTOM_METADATA_KEY_DURATION = "__DURATION__";
    String CUSTOM_METADATA_KEY_FIRST_YEAR = "__FIRST_YEAR__";
    String CUSTOM_METADATA_KEY_LAST_YEAR = "__LAST_YEAR__";
    String CUSTOM_METADATA_KEY_NUM_ALBUMS = "__NUM_ALBUMS__";
    String CUSTOM_METADATA_KEY_NUM_TRACKS = "__NUM_TRACKS__";

    String CUSTOM_METADATA_KEY_SEARCH_ITEM_TYPE = "__SEARCH_ITEM_TYPE__";

    String SEARCH_RESULT_ITEM_TYPE_SONG = "Songs";
    String SEARCH_RESULT_ITEM_TYPE_ALBUM = "Albums";
    String SEARCH_RESULT_ITEM_TYPE_ARTIST = "Artists";

    Iterator<MediaMetadataCompat> getAllSongsIterator();
    Iterator<MediaMetadataCompat> getAlbumsIterator();
    Iterator<MediaMetadataCompat> getArtistsIterator();
}
