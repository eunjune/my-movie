package com.example.mymovie.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.example.mymovie.activity.ActivityActionListener;
import com.example.mymovie.MyFunction;
import com.example.mymovie.R;
import com.example.mymovie.data.MovieDetailInfo;
import com.example.mymovie.data.MovieDetailList;
import com.example.mymovie.data.MovieInfo;
import com.example.mymovie.data.ProtocolObj;
import com.example.mymovie.data.ResponseInfo;
import com.example.mymovie.database.DBHelper;
import com.example.mymovie.network.ImageLoadTask;
import com.example.mymovie.network.NetworkManager;

public class MoviePosterFragment extends Fragment {

    private ActivityActionListener activityActionListener;
    private NetworkManager networkManager;

    private MovieDetailFragment movieDetailFragment;
    private ImageView ivPoster;
    private TextView tvTitle, tvReservationRate, tvGrade;

    private MovieInfo movieInfo;
    private int index;

    public MovieDetailFragment getMovieDetailFragment() {
        return movieDetailFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = getActivity();
        if(activity instanceof ActivityActionListener) {
            activityActionListener = (ActivityActionListener)activity;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_movie_poster, container, false);

        initFields();
        initView(rootView);
        //requestMovie();

        return rootView;
    }


    // 영화 상세 정보
    private void showMovieDetailFragment(Bundle bundle) {

        movieDetailFragment.setArguments(bundle);

        FragmentManager manager = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        transaction.replace(R.id.container, movieDetailFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void initFields() {
        movieDetailFragment = new MovieDetailFragment();
        index = getArguments().getInt("index");
        movieInfo = (MovieInfo) getArguments().getSerializable("movieItem");
        networkManager = new NetworkManager(getContext());
    }

    private void initView(ViewGroup rootView) {
        activityActionListener.setTitle(getString(R.string.all_movie_list));

        // 뷰 세팅
        ivPoster = (ImageView) rootView.findViewById(R.id.iv_poster);

        if(networkManager.isNetworkAvailable()) {
            ImageLoadTask imageLoadTask = new ImageLoadTask(movieInfo.getImage(),ivPoster);
            imageLoadTask.execute();
        }


        tvTitle = (TextView) rootView.findViewById(R.id.tv_title);
        tvTitle.setText((index + 1) + "." + movieInfo.getTitle());

        tvReservationRate = (TextView) rootView.findViewById(R.id.tv_reservation_rate);
        String strReservationRate = getString(R.string.all_reservation_rate) + movieInfo.getReservation_rate() + "%";
        tvReservationRate.setText(strReservationRate);

        tvGrade = (TextView) rootView.findViewById(R.id.tv_grade);
        String strGrade = String.valueOf(movieInfo.getGrade()) + getString(R.string.movie_poster_grade);
        tvGrade.setText(strGrade);


        Button btnDetail = (Button) rootView.findViewById(R.id.btn_detail);
        btnDetail.setOnClickListener(new View.OnClickListener() {

            // 상세보기 클릭시
            @Override
            public void onClick(View v) {
                requestMovie();
            }
        });
    }

    private void requestMovie() {
        
        if(networkManager.isNetworkAvailable()) {
            final ProtocolObj protocolObj = new ProtocolObj();
            protocolObj.setUrl("readMovie");
            protocolObj.setRequestType(Request.Method.GET);
            protocolObj.setParam("name",movieInfo.getTitle());
            protocolObj.setResponseClass(MovieDetailList.class);

            MyFunction myFunction = new MyFunction() {
                @Override
                public void callback(String response) {

                    ResponseInfo responseInfo = protocolObj.getResponseInfo(response);

                    if(responseInfo.code == 200) {
                        MovieDetailList movieDetailList = (MovieDetailList) protocolObj
                                .getResponseClass(response);
                        MovieDetailInfo movieDetailInfo = movieDetailList.getMovieDetailInfo(0);

                        if(DBHelper.selectMovieCount(movieDetailInfo.getId()) == 0) {
                            DBHelper.insertMovie(movieDetailInfo);
                        } else {
                            DBHelper.updateMovie(movieDetailInfo);
                        }

                        if(movieDetailInfo != null) {
                            Bundle bundle = new Bundle();
                            bundle.putSerializable("movieDetailInfo", movieDetailInfo);
                            bundle.putInt("index",index);
                            showMovieDetailFragment(bundle);
                        }
                    } else if(responseInfo.code == 400) {
                        String message = getResources().getString(R.string.all_load_error);
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    } else {
                        String message = getResources().getString(R.string.all_unknown_error);
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    }
                }
            };

            networkManager.request(protocolObj,getContext(), myFunction);
        } else {
            MovieDetailInfo movieDetailInfo = DBHelper.selectMovie(movieInfo.getId());
            if(movieDetailInfo != null) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("movieDetailInfo", movieDetailInfo);
                bundle.putInt("index",index);
                showMovieDetailFragment(bundle);
            }
        }
    }

}
