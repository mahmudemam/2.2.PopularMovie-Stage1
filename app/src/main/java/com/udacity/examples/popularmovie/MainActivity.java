package com.udacity.examples.popularmovie;


import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.udacity.examples.popularmovie.adapters.MovieCursorAdapter;
import com.udacity.examples.popularmovie.adapters.MovieListAdapter;
import com.udacity.examples.popularmovie.adapters.MoviesAdapter;
import com.udacity.examples.popularmovie.data.Movie;
import com.udacity.examples.popularmovie.tasks.MovieAsyncTaskLoader;
import com.udacity.examples.popularmovie.utils.ContentProviderUtils;
import com.udacity.examples.popularmovie.utils.JsonUtils;
import com.udacity.examples.popularmovie.utils.NetworkUtils;

import java.util.List;

public class MainActivity extends AppCompatActivity implements MoviesAdapter.OnMovieClickListener, MovieAsyncTaskLoader.MovieAsyncTaskLoaderListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SORT_ORDER_KEY = "SORT_ORDER";
    private static final String RV_POSITION_KEY = "RV_KEY";
    private RecyclerView moviesRecyclerView;
    private ProgressBar progressBar;
    private int sort_order = FetchingMovieTask.POPULAR_MOVIES_ID;
    private static final int MOVIE_TASK_LOADER_ID = 1;

    private MoviesAdapter adapter;
    private Object movies;
    private Parcelable parcelable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final String M = "onCreate: ";
        Log.d(TAG, M + "Start");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        moviesRecyclerView = findViewById(R.id.rv_movies);
        progressBar = findViewById(R.id.pb_loading_bar);

        if (NetworkUtils.isNetworkActive(this)) {
            if (savedInstanceState != null && savedInstanceState.containsKey(SORT_ORDER_KEY)) {
                sort_order = savedInstanceState.getInt(SORT_ORDER_KEY);
            }
            new FetchingMovieTask().execute();
        } else {
            Toast.makeText(this, "Network Connection is not Active, favorites are displayed", Toast.LENGTH_SHORT).show();
            sort_order = FetchingMovieTask.FAVORITE_MOVIES_ID;
            new FetchingMovieTask().execute();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SORT_ORDER_KEY, sort_order);
        if (moviesRecyclerView != null && moviesRecyclerView.getLayoutManager() != null) {
            outState.putParcelable(RV_POSITION_KEY, moviesRecyclerView.getLayoutManager().onSaveInstanceState());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        final String M = "onRestoreInstanceState: ";
        Log.d(TAG, M + "Start");
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SORT_ORDER_KEY)) {
                sort_order = savedInstanceState.getInt(SORT_ORDER_KEY);
            }

            if (savedInstanceState.containsKey(RV_POSITION_KEY)) {
                parcelable = savedInstanceState.getParcelable(RV_POSITION_KEY);
            }
        }
    }

    @Override
    public void onImageClicked(Movie movie) {
        Log.d(TAG, "Listener: " + movie.toString());

        Intent detailsIntent = new Intent(this, DetailsActivity.class);
        detailsIntent.putExtra(DetailsActivity.INTENT_MOVIE_KEY, movie);

        startActivity(detailsIntent);
    }

    @Override
    public void onFavoritePressed(Movie movie, boolean selected) {
        if (selected) {
            ContentProviderUtils.addFavoriteMovie(this, movie);

            Bundle bundle = new Bundle();
            bundle.putParcelable(MovieAsyncTaskLoader.ASYNC_LOADER_MOVIE_BUNDLE_ID, movie);

            LoaderManager loaderManager = getSupportLoaderManager();
            if (loaderManager.getLoader(MOVIE_TASK_LOADER_ID) != null) {
                getSupportLoaderManager().restartLoader(MOVIE_TASK_LOADER_ID, bundle, new MovieAsyncTaskLoader(this, this));
            } else {
                getSupportLoaderManager().initLoader(MOVIE_TASK_LOADER_ID, bundle, new MovieAsyncTaskLoader(this, this));
            }
        } else
            ContentProviderUtils.removeFavoriteMovie(this, movie);

        new FetchingMovieTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_popular_movies:
                sort_order = FetchingMovieTask.POPULAR_MOVIES_ID;
                new FetchingMovieTask().execute();
                return true;
            case R.id.menu_top_rated_movies:
                sort_order = FetchingMovieTask.TOP_RATED_MOVIES_ID;
                new FetchingMovieTask().execute();
                return true;
            case R.id.menu_favorite_movies:
                sort_order = FetchingMovieTask.FAVORITE_MOVIES_ID;
                new FetchingMovieTask().execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void notify(Movie movie) {
        ContentProviderUtils.addMovieVideos(this, movie.getId(), movie.getVideos());
        ContentProviderUtils.addMovieReviews(this, movie.getId(), movie.getReviews());
    }

    public class FetchingMovieTask extends AsyncTask<Void, Void, Object> {
        static final int POPULAR_MOVIES_ID = 1;
        static final int TOP_RATED_MOVIES_ID = 2;
        static final int FAVORITE_MOVIES_ID = 3;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Object doInBackground(Void... voids) {
            if (sort_order == POPULAR_MOVIES_ID)
                return NetworkUtils.loadPopularMovies();
            else if (sort_order == TOP_RATED_MOVIES_ID)
                return NetworkUtils.loadTopRatedMovies();
            else if (sort_order == FAVORITE_MOVIES_ID)
                return ContentProviderUtils.getFavoriteMovies(MainActivity.this);
            else return null;
        }

        @Override
        protected void onPostExecute(Object data) {
            progressBar.setVisibility(View.INVISIBLE);

            if (sort_order != FAVORITE_MOVIES_ID) {
                movies = JsonUtils.parseMovies((String) data);
                adapter = new MovieListAdapter(MainActivity.this, MainActivity.this, (List<Movie>) movies);
            } else {
                movies = data;
                adapter = new MovieCursorAdapter(MainActivity.this, MainActivity.this, (Cursor) movies);
            }
            moviesRecyclerView.setAdapter(adapter);

            moviesRecyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this, 3));
            moviesRecyclerView.setHasFixedSize(true);

            moviesRecyclerView.getLayoutManager().onRestoreInstanceState(parcelable);
        }
    }
}
