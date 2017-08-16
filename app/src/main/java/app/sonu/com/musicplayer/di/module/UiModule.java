package app.sonu.com.musicplayer.di.module;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;

import javax.inject.Named;

import app.sonu.com.musicplayer.data.DataManager;
import app.sonu.com.musicplayer.di.ActivityContext;
import app.sonu.com.musicplayer.di.PerActivity;
import app.sonu.com.musicplayer.mediaplayernew.util.MediaIdHelper;
import app.sonu.com.musicplayer.ui.albums.AlbumsMvpPresenter;
import app.sonu.com.musicplayer.ui.albums.AlbumsPresenter;
import app.sonu.com.musicplayer.ui.allsongs.AllSongsMvpPresenter;
import app.sonu.com.musicplayer.ui.allsongs.AllSongsPresenter;
import app.sonu.com.musicplayer.ui.artists.ArtistsMvpPresenter;
import app.sonu.com.musicplayer.ui.artists.ArtistsPresenter;
import app.sonu.com.musicplayer.mediaplayernew.manager.MediaBrowserManager;
import app.sonu.com.musicplayer.ui.fileview.FileViewMvpPresenter;
import app.sonu.com.musicplayer.ui.fileview.FileViewPresenter;
import app.sonu.com.musicplayer.ui.main.MainMvpPresenter;
import app.sonu.com.musicplayer.ui.main.MainPresenter;

import app.sonu.com.musicplayer.ui.miniplayer.MiniPlayerMvpPresenter;
import app.sonu.com.musicplayer.ui.miniplayer.MiniPlayerPresenter;
import app.sonu.com.musicplayer.ui.musicplayer.MusicPlayerMvpPresenter;
import app.sonu.com.musicplayer.ui.musicplayer.MusicPlayerPresenter;
import dagger.Module;
import dagger.Provides;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by sonu on 29/6/17.
 */

@Module
public class UiModule {
    private Context mContext;

    public UiModule(Context context) {
        this.mContext = context;
    }

    @Provides
    @PerActivity
    @ActivityContext
    Context getContext() {
        return this.mContext;
    }

    @Provides
    @PerActivity
    MainMvpPresenter getMainMvpPresenter(MainPresenter mainPresenter) {
        return mainPresenter;
    }

    @Provides
    @PerActivity
    MainPresenter getMainPresenter(DataManager dataManager,
                                   @Named(BusModule.PROVIDER_SELECTED_SONG)
                                   PublishSubject<MediaBrowserCompat.MediaItem> selectedSongSubject) {
        return new MainPresenter(dataManager, selectedSongSubject);
    }

    @Provides
    @PerActivity
    FileViewMvpPresenter getFileViewMvpPresenter(FileViewPresenter fileViewPresenter) {
        return fileViewPresenter;
    }

    @Provides
    @PerActivity
    FileViewPresenter getFileViewPresenter(DataManager dataManager) {
        return new FileViewPresenter(dataManager);
    }

    @Provides
    @PerActivity
    AllSongsMvpPresenter getAllSongsMvpPresenter(AllSongsPresenter allSongsPresenter) {
        return allSongsPresenter;
    }

    @Provides
    @PerActivity
    MiniPlayerPresenter getMiniPlayerPresenter(DataManager dataManager,
                                               @Named(BusModule.PROVIDER_MUSIC_PLAYER_PANEL)
                                                       PublishSubject<Integer>
                                                       musicPlayerPanelPublishSubject) {
        return  new MiniPlayerPresenter(dataManager, new MediaBrowserManager(null),
                musicPlayerPanelPublishSubject);
    }

    @Provides
    @PerActivity
    MiniPlayerMvpPresenter getMiniPlayerMvpPresenter(MiniPlayerPresenter miniPlayerPresenter) {
        return miniPlayerPresenter;
    }

    @Provides
    @PerActivity
    AllSongsPresenter getAllSongsPresenter(DataManager dataManager,
                                           @Named(BusModule.PROVIDER_SELECTED_SONG)
                                                   PublishSubject<MediaBrowserCompat.MediaItem> selectedSongSubject) {
        return new AllSongsPresenter(dataManager,
                new MediaBrowserManager(MediaIdHelper.ALL_SONGS_ROOT_HINT),
                selectedSongSubject);
    }

    @Provides
    @PerActivity
    MusicPlayerMvpPresenter getMusicPlayerMvpPresenter(MusicPlayerPresenter musicPlayerPresenter) {
        return musicPlayerPresenter;
    }

    @Provides
    @PerActivity
    MusicPlayerPresenter getMusicPlayerPresenter(DataManager dataManager,
                                                 @Named(BusModule.PROVIDER_SELECTED_SONG)
                                                         PublishSubject<MediaBrowserCompat.MediaItem>
                                                         selectedSongSubject,
                                                 @Named(BusModule.PROVIDER_PLAY_SONG)
                                                         PublishSubject<MediaBrowserCompat.MediaItem>
                                                         playSongSubject,
                                                 @Named(BusModule.PROVIDER_MUSIC_PLAYER_SLIDE)
                                                         PublishSubject<Float>
                                                         musicPlayerSlideSubject,
                                                 @Named(BusModule.PROVIDER_MUSIC_PLAYER_PANEL)
                                                         PublishSubject<Integer>
                                                         musicPlayerPanelPublishSubject) {
        return new MusicPlayerPresenter(dataManager, selectedSongSubject, playSongSubject,
                musicPlayerSlideSubject, new MediaBrowserManager(null),
                musicPlayerPanelPublishSubject);
    }

    @Provides
    @PerActivity
    AlbumsMvpPresenter getAlbumsMvpPresenter(AlbumsPresenter albumsPresenter) {
        return albumsPresenter;
    }

    @Provides
    @PerActivity
    AlbumsPresenter getAlbumsPresenter(DataManager dataManager) {
        return new AlbumsPresenter(dataManager,
                new MediaBrowserManager(MediaIdHelper.ALBUMS_ROOT_HINT));
    }

    @Provides
    @PerActivity
    ArtistsMvpPresenter getArtistsMvpPresenter(ArtistsPresenter artistsPresenter) {
        return artistsPresenter;
    }

    @Provides
    @PerActivity
    ArtistsPresenter getArtistsPresenter(DataManager dataManager) {
        return new ArtistsPresenter(dataManager,
                new MediaBrowserManager(MediaIdHelper.ARTISTS_ROOT_HINT));
    }
}
