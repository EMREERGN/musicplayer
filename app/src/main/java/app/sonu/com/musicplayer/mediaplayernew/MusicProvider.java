package app.sonu.com.musicplayer.mediaplayernew;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import app.sonu.com.musicplayer.mediaplayernew.musicsource.MusicProviderSource;
import app.sonu.com.musicplayer.mediaplayernew.util.MediaIdHelper;

/**
 * responsible for providing music for a specified music source
 */
public class MusicProvider {
    private static final String TAG = MusicProvider.class.getSimpleName();

    // source from where music is taken
    private MusicProviderSource mSource;

    // caches of music list
    private final ConcurrentMap<String, List<MediaMetadataCompat>> mMusicListByArtistKey;
    private final ConcurrentMap<String, List<MediaMetadataCompat>> mMusicListByAlbumKey;

    private final ConcurrentMap<String, MediaMetadataCompat> mArtistListByKey;
    private final ConcurrentMap<String, MediaMetadataCompat> mAlbumListByKey;
    private final ConcurrentMap<String, MediaMetadataCompat> mMusicListById;
    private final ConcurrentHashMap<String, String> mArtistArtByKey;

    private ArrayList<MediaMetadataCompat> allSongs;
    private ArrayList<MediaMetadataCompat> allAlbums;
    private ArrayList<MediaMetadataCompat> allArtists;

    //comparing two metadata on the basis of their display title
    private static final Comparator<MediaMetadataCompat> mediaMetadataComparator =
            new Comparator<MediaMetadataCompat>() {
                @Override
                public int compare(MediaMetadataCompat o1, MediaMetadataCompat o2) {
                    return o1
                            .getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
                            .compareTo(
                                    o2.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE));
                }
            };

    @SuppressWarnings("WeakerAccess")
    public enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    //defining volatile to make it thread safe but not blocking
    private volatile State mCurrentState = State.NON_INITIALIZED;

    @SuppressWarnings("WeakerAccess")
    public MusicProvider(MusicProviderSource musicProviderSource) {
        this.mSource = musicProviderSource;

        this.mMusicListById = new ConcurrentHashMap<>();
        this.mArtistListByKey = new ConcurrentHashMap<>();
        this.mAlbumListByKey = new ConcurrentHashMap<>();
        this.mMusicListByAlbumKey = new ConcurrentHashMap<>();
        this.mMusicListByArtistKey = new ConcurrentHashMap<>();
        this.mArtistArtByKey = new ConcurrentHashMap<>();
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     * @param callback callback method ia called then done retrieving media
     */
    @SuppressWarnings("WeakerAccess")
    public void retrieveMediaAsync(final Callback callback) {
        Log.d(TAG, "retrieveMediaAsync:called");
        if (mCurrentState == State.INITIALIZED) {
            if (callback != null) {
                // Nothing to do, execute callback immediately
                callback.onMusicCatalogReady(true);
            }
            return;
        }

        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, State>() {
            @Override
            protected State doInBackground(Void... params) {
                retrieveMedia();
                return mCurrentState;
            }

            @Override
            protected void onPostExecute(State current) {
                if (callback != null) {
                    callback.onMusicCatalogReady(current == State.INITIALIZED);
                }
            }
        }.execute();
    }

    /**
     * retrieves media, synchronized to make it thread safe
     */
    private synchronized void retrieveMedia() {
        Log.d(TAG, "retrieveMedia:called");
        try {
            if (mCurrentState == State.NON_INITIALIZED) {
                mCurrentState = State.INITIALIZING;

                Iterator<MediaMetadataCompat> allSongsIterator = mSource.getAllSongsIterator();
                Iterator<MediaMetadataCompat> albumsIterator = mSource.getAlbumsIterator();
                Iterator<MediaMetadataCompat> artistsIterator = mSource.getArtistsIterator();

                // making music caches

                // albumlistbykey has to be formed before making allsongs list
                // album art from this list is used for making song lists
                while (albumsIterator.hasNext()) {
                    MediaMetadataCompat item = albumsIterator.next();
                    mAlbumListByKey.put(
                            item.getString(MusicProviderSource.CUSTOM_METADATA_KEY_ALBUM_KEY),
                            item
                    );
                }

                while (allSongsIterator.hasNext()) {
                    MediaMetadataCompat item = allSongsIterator.next();
                    String musicId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);

                    //setting album art to the song
                    item = setAlbumArt(item);

                    mMusicListById.put(musicId, item);

                    // adding the current song to its album cache
                    String albumKey = item
                            .getString(MusicProviderSource.CUSTOM_METADATA_KEY_ALBUM_KEY);
                    String artistKey = item
                            .getString(MusicProviderSource.CUSTOM_METADATA_KEY_ARTIST_KEY);
                    if (!mMusicListByAlbumKey.containsKey(albumKey)) {
                        mMusicListByAlbumKey.put(albumKey, new ArrayList<MediaMetadataCompat>());
                    }
                    mMusicListByAlbumKey.get(albumKey).add(item);

                    String albumArtUri =
                            item.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);

                    if (albumArtUri != null) {
//                        Log.d(TAG, "album art uri is not null for song");
                        if (!mArtistArtByKey.containsKey(artistKey)) {
//                            Log.d(TAG, "album artist art by key does not contain key");
//                            Log.i(TAG, "putting "+artistKey+" "+albumArtUri);
                            mArtistArtByKey.put(artistKey, albumArtUri);
                        }
                    }

                    // adding current song to its artist cache
                    if (!mMusicListByArtistKey.containsKey(artistKey)) {
                        mMusicListByArtistKey.put(artistKey, new ArrayList<MediaMetadataCompat>());
                    }
                    mMusicListByArtistKey.get(artistKey).add(item);
                }

                while (artistsIterator.hasNext()) {
                    MediaMetadataCompat item = artistsIterator.next();
                    mArtistListByKey.put(
                            item.getString(MusicProviderSource.CUSTOM_METADATA_KEY_ARTIST_KEY),
                            setAlbumArtForArtist(item)
                    );
                }

                // initializing <all> lists cache
                allSongs = new ArrayList<>(mMusicListById.values());
                allAlbums = new ArrayList<>(mAlbumListByKey.values());
                allArtists = new ArrayList<>(mArtistListByKey.values());

                // sorting all the <all> lists according to their display title
                Collections.sort(allSongs, mediaMetadataComparator);
                Collections.sort(allAlbums, mediaMetadataComparator);
                Collections.sort(allArtists, mediaMetadataComparator);

                mCurrentState = State.INITIALIZED;
            }
        } catch (Exception e){
            Log.e(TAG, "retrieveMedia:", e);
            e.printStackTrace();
        } finally {
            if (mCurrentState != State.INITIALIZED) {
                Log.w(TAG, "retrieveMedia:state is not initialized");
                // setting state to non-initialized to allow retires
                // if something bad happened
                mCurrentState = State.NON_INITIALIZED;
            }
        }
    }

    /**
     * setting album art for a song according to its album key
     * @param item old mediametadata
     * @return new mediametadata with the album art
     */
    private MediaMetadataCompat setAlbumArt(MediaMetadataCompat item) {
        // we create a new object as MediaMetadataCompat is immutable
        return new MediaMetadataCompat
                .Builder(item)
                .putString(
                        MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                        mAlbumListByKey.get(
                                item.getString(MusicProviderSource.CUSTOM_METADATA_KEY_ALBUM_KEY)
                        ).getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
                )
                .build();
    }

    private MediaMetadataCompat setAlbumArtForArtist(MediaMetadataCompat item) {
        // we create a new object as MediaMetadataCompat is immutable
        return new MediaMetadataCompat
                .Builder(item)
                .putString(
                        MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                        mArtistArtByKey.get(
                                item.getString(MusicProviderSource.CUSTOM_METADATA_KEY_ARTIST_KEY)
                        )
                )
                .build();
    }


    /**
     * tells is the provider is initialized or not
     * @return true if initialized else false
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isInitialized() {
        return this.mCurrentState == State.INITIALIZED;
    }

    /**
     * returns children for given root media id
     * @param mediaId root media id
     * @return list of children
     */
    @SuppressWarnings("WeakerAccess")
    public List<MediaBrowserCompat.MediaItem> getChildren(String mediaId) {
        Log.d(TAG, "getChildren:called mediaId="+mediaId);

        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        if (!MediaIdHelper.isBrowseable(mediaId)) {
            return mediaItems;
        }

        if (MediaIdHelper.MEDIA_ID_ALL_SONGS.equals(mediaId)) {
            for (MediaMetadataCompat metadata : getSongs()) {
                mediaItems.add(
                        createMediaItem(
                                metadata,
                                MediaIdHelper.MEDIA_ID_ALL_SONGS,
                                null)
                );
            }
        } else if (MediaIdHelper.MEDIA_ID_ALBUMS.equals(mediaId)) {
            for (MediaMetadataCompat album : getAlbums()) {
                mediaItems.add(createBrowsableMediaItemForAlbum(album));
            }
        } else if (MediaIdHelper.MEDIA_ID_ARTISTS.equals(mediaId)) {
            for (MediaMetadataCompat artist : getArtists()) {
                mediaItems.add(createBrowsableMediaItemForArtist(artist));
            }
        } else if (mediaId.startsWith(MediaIdHelper.MEDIA_ID_ALBUMS)) {
            String album = MediaIdHelper.getHierarchy(mediaId)[1];
            for (MediaMetadataCompat metadata : getMusicsByAlbumKey(album)) {
                mediaItems.add(createMediaItem(metadata,
                        MediaIdHelper.MEDIA_ID_ALBUMS,
                        MusicProviderSource.CUSTOM_METADATA_KEY_ALBUM_KEY));
            }
        } else if (mediaId.startsWith(MediaIdHelper.MEDIA_ID_ARTISTS)) {
            String artist = MediaIdHelper.getHierarchy(mediaId)[1];
            for (MediaMetadataCompat metadata : getMusicsByArtistKey(artist)) {
                mediaItems.add(createMediaItem(metadata,
                        MediaIdHelper.MEDIA_ID_ARTISTS,
                        MusicProviderSource.CUSTOM_METADATA_KEY_ARTIST_KEY));
            }
        } else {
            Log.w(TAG, "getChildren:skipping unmatched mediaId: "+mediaId);
        }
        return mediaItems;
    }

    public Iterable<MediaMetadataCompat> getAlbums() {
        Log.d(TAG, "getAlbums:called");
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }

        return allAlbums;
    }

    public Iterable<MediaMetadataCompat> getSongs() {
        Log.d(TAG, "getSongs:called");
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }

        return allSongs;
    }

    public Iterable<MediaMetadataCompat> getArtists() {
        Log.d(TAG, "getArtists:called");
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        Log.i(TAG, "getArtists:noOfArtists="+mArtistListByKey.values().size());

        return allArtists;
    }

    public Iterable<MediaMetadataCompat> getMusicsByAlbumKey(String albumKey) {
        Log.d(TAG, "getMusicsByAlbumKey:called");
        if (mCurrentState != State.INITIALIZED || !mMusicListByAlbumKey.containsKey(albumKey)) {
            return Collections.emptyList();
        }
        return mMusicListByAlbumKey.get(albumKey);
    }

    public Iterable<MediaMetadataCompat> getMusicsByArtistKey(String artistKey) {
        Log.d(TAG, "getMusicsByArtistKey:called");
        if (mCurrentState != State.INITIALIZED || !mMusicListByArtistKey.containsKey(artistKey)) {
            return Collections.emptyList();
        }
        return mMusicListByArtistKey.get(artistKey);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForAlbum(
            MediaMetadataCompat metadata) {

        Bundle extras = new Bundle();

        extras.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));

        extras.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS,
                metadata.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS));

        String hierarchyAwareMediaId = MediaIdHelper.createMediaId(
                null,
                MediaIdHelper.MEDIA_ID_ALBUMS,
                metadata.getString(MusicProviderSource.CUSTOM_METADATA_KEY_ALBUM_KEY)
        );

        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                .setExtras(extras)
                .setMediaId(hierarchyAwareMediaId)
                .setTitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE))
                .setSubtitle(metadata.getString(
                        MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE));

        if (metadata.getString(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI) != null) {
            builder.setIconUri(
                    Uri.parse(
                            metadata.getString(
                                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)));
        }

        MediaDescriptionCompat descriptionCompat = builder.build();

        return new MediaBrowserCompat.MediaItem(descriptionCompat,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForArtist(
            MediaMetadataCompat metadata) {

        String hierarchyAwareMediaId = MediaIdHelper.createMediaId(
                null,
                MediaIdHelper.MEDIA_ID_ARTISTS,
                metadata.getString(MusicProviderSource.CUSTOM_METADATA_KEY_ARTIST_KEY)
        );

        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                .setMediaId(hierarchyAwareMediaId)
                .setTitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE))
                .setSubtitle(metadata.getString(
                        MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE));

        if (metadata.getString(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI) != null) {
            builder.setIconUri(
                    Uri.parse(
                            metadata.getString(
                                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)));
        }

        MediaDescriptionCompat descriptionCompat = builder.build();

        return new MediaBrowserCompat.MediaItem(descriptionCompat,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createMediaItem(
            MediaMetadataCompat metadata,
            String byMediaId,
            String byMetadataKey) {
        // Since mediaMetadata fields are immutable, we need to create a copy, so we
        // can set a hierarchy-aware mediaID. We will need to know the media hierarchy
        // when we get a onPlayFromMusicID call, so we can create the proper queue based
        // on where the music was selected from (by artist, by genre, random, etc)

        Bundle extras = new Bundle();

        extras.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
        extras.putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM));
        extras.putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
        extras.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER,
                metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER));

        String hierarchyAwareMediaId, subtitle="";

        if (byMediaId.equals(MediaIdHelper.MEDIA_ID_ALL_SONGS)) {
            hierarchyAwareMediaId = MediaIdHelper.createMediaId(
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID),
                    byMediaId);
            subtitle = metadata.getString(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE);
        } else {
            hierarchyAwareMediaId = MediaIdHelper.createMediaId(
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID),
                    byMediaId,
                    metadata.getString(byMetadataKey));

            if (byMediaId.startsWith(MediaIdHelper.MEDIA_ID_ARTISTS)) {
                subtitle = metadata.getString(
                        MediaMetadataCompat.METADATA_KEY_ALBUM);
            } else if (byMediaId.startsWith(MediaIdHelper.MEDIA_ID_ALBUMS)) {
                subtitle = metadata.getString(
                        MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE);
            }
        }

        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                .setExtras(extras)
                .setMediaId(hierarchyAwareMediaId)
                .setTitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE))
                .setSubtitle(subtitle);

        if (metadata.getString(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI) != null) {
            builder.setIconUri(
                    Uri.parse(
                            metadata.getString(
                                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)));
        }

        MediaDescriptionCompat descriptionCompat = builder.build();

        return new MediaBrowserCompat.MediaItem(descriptionCompat,
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

    }

    private MediaBrowserCompat.MediaItem createSearchMediaItem(
            MediaMetadataCompat metadata,
            String byMediaId) {
        // Since mediaMetadata fields are immutable, we need to create a copy, so we
        // can set a hierarchy-aware mediaID. We will need to know the media hierarchy
        // when we get a onPlayFromMusicID call, so we can create the proper queue based
        // on where the music was selected from (by artist, by genre, random, etc)

        if (byMediaId.equals(MediaIdHelper.MEDIA_ID_ALL_SONGS)) {
            Bundle extras = new Bundle();

            extras.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            extras.putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM));
            extras.putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                    metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
            extras.putString(
                    MusicProviderSource.CUSTOM_METADATA_KEY_SEARCH_ITEM_TYPE,
                    MusicProviderSource.SEARCH_RESULT_ITEM_TYPE_SONG);

            String hierarchyAwareMediaId;

            hierarchyAwareMediaId = MediaIdHelper.createMediaId(
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID),
                    byMediaId);

            MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                    .setExtras(extras)
                    .setMediaId(hierarchyAwareMediaId)
                    .setTitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE))
                    .setSubtitle(metadata.getString(
                            MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE));

            if (metadata.getString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI) != null) {
                builder.setIconUri(
                        Uri.parse(
                                metadata.getString(
                                        MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)));
            }

            MediaDescriptionCompat descriptionCompat = builder.build();

            return new MediaBrowserCompat.MediaItem(descriptionCompat,
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

        } else if (byMediaId.equals(MediaIdHelper.MEDIA_ID_ALBUMS)) {
            return createSearchBrowsableMediaItemForAlbum(metadata);
        } else if (byMediaId.equals(MediaIdHelper.MEDIA_ID_ARTISTS)) {
            return createSearchBrowsableMediaItemForArtist(metadata);
        }

        return null;
    }

    private MediaBrowserCompat.MediaItem createSearchBrowsableMediaItemForArtist(
            MediaMetadataCompat metadata) {

        String hierarchyAwareMediaId = MediaIdHelper.createMediaId(
                null,
                MediaIdHelper.MEDIA_ID_ARTISTS,
                metadata.getString(MusicProviderSource.CUSTOM_METADATA_KEY_ARTIST_KEY)
        );

        Bundle extras = new Bundle();
        extras.putString(
                MusicProviderSource.CUSTOM_METADATA_KEY_SEARCH_ITEM_TYPE,
                MusicProviderSource.SEARCH_RESULT_ITEM_TYPE_ARTIST);

        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                .setExtras(extras)
                .setMediaId(hierarchyAwareMediaId)
                .setTitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE))
                .setSubtitle(metadata.getString(
                        MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE));

        MediaDescriptionCompat descriptionCompat = builder.build();

        return new MediaBrowserCompat.MediaItem(descriptionCompat,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createSearchBrowsableMediaItemForAlbum(
            MediaMetadataCompat metadata) {

        String hierarchyAwareMediaId = MediaIdHelper.createMediaId(
                null,
                MediaIdHelper.MEDIA_ID_ALBUMS,
                metadata.getString(MusicProviderSource.CUSTOM_METADATA_KEY_ALBUM_KEY)
        );

        Bundle extras = new Bundle();
        extras.putString(
                MusicProviderSource.CUSTOM_METADATA_KEY_SEARCH_ITEM_TYPE,
                MusicProviderSource.SEARCH_RESULT_ITEM_TYPE_ALBUM);

        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                .setExtras(extras)
                .setMediaId(hierarchyAwareMediaId)
                .setTitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE))
                .setSubtitle(metadata.getString(
                        MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE));

        MediaDescriptionCompat descriptionCompat = builder.build();

        return new MediaBrowserCompat.MediaItem(descriptionCompat,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    public MediaMetadataCompat getMusic(String musicId) {
        return mMusicListById.containsKey(musicId) ? mMusicListById.get(musicId) : null;
    }

    public List<MediaBrowserCompat.MediaItem> getSongsBySearchQuery(String query) {
        ArrayList<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        for (MediaMetadataCompat metadata : getSongs()) {
            if (metadata
                        .getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
                        .toLowerCase()
                        .replaceAll("\\s","")
                        .contains(query)
                    ) {
                mediaItems.add(
                        createSearchMediaItem(
                                metadata,
                                MediaIdHelper.MEDIA_ID_ALL_SONGS)
                );
            }
        }

        for (MediaMetadataCompat metadata : getAlbums()) {
            if (metadata
                    .getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
                    .toLowerCase()
                    .replaceAll("\\s","")
                    .contains(query)
                    ) {
                mediaItems.add(
                        createSearchMediaItem(
                                metadata,
                                MediaIdHelper.MEDIA_ID_ALBUMS)
                );
            }
        }

        for (MediaMetadataCompat metadata : getArtists()) {
            if (metadata
                    .getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
                    .toLowerCase()
                    .replaceAll("\\s","")
                    .contains(query)
                    ) {
                mediaItems.add(
                        createSearchMediaItem(
                                metadata,
                                MediaIdHelper.MEDIA_ID_ARTISTS)
                );
            }
        }
        return mediaItems;
    }

    public interface Callback {
        void onMusicCatalogReady(boolean success);
    }
}