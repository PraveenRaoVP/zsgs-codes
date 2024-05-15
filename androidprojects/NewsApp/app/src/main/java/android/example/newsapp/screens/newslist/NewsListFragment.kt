package android.example.newsapp.screens.newslist

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.example.newsapp.R
import android.example.newsapp.adapters.ListItemAdapter
import android.example.newsapp.adapters.NewsListAdapter
import android.example.newsapp.database.NewsDatabase
import android.example.newsapp.databinding.FragmentNewsListBinding
import android.example.newsapp.screens.enlargeimage.EnlargeImageDialog
import android.example.newsapp.utils.ImageClickListener
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso


class NewsListFragment : Fragment(), ImageClickListener {

    private lateinit var newsListViewModel: NewsListViewModel
    private lateinit var newsRecyclerView: RecyclerView
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var binding: FragmentNewsListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val application = requireNotNull(this.activity).application
        val dataSource = NewsDatabase.getInstance(application).newsDao()

        val viewModelFactory = NewsListViewModelFactory(dataSource, application)
        newsListViewModel = ViewModelProvider(this, viewModelFactory)[NewsListViewModel::class.java]
        Log.i("NewsListFragment", "currentCategory is ${newsListViewModel.currentCategory.value}")

        newsListViewModel.populateDataFromDatabase(newsListViewModel.currentCategory.value ?: "all")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentNewsListBinding.inflate(inflater, container, false)

        newsRecyclerView = binding.newsListRecyclerView
        val newsItemAdapter = NewsListAdapter(newsListViewModel, this)
        newsRecyclerView.adapter = newsItemAdapter
        newsRecyclerView.layoutManager = LinearLayoutManager(this.context)


        categoryRecyclerView = binding.categoryRecyclerView
        val categoryItemAdapter = ListItemAdapter(newsListViewModel)
        categoryRecyclerView.adapter = categoryItemAdapter
        categoryRecyclerView.layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.HORIZONTAL, false)
        categoryItemAdapter.submitList(newsListViewModel.categories)

        binding.lifecycleOwner = this

        requestLocationPermission()

        newsListViewModel.categoryClicked.observe(viewLifecycleOwner) {
            if(it) {
                Log.i("NewsListFragment", "categoryClicked with category ${newsListViewModel.currentCategory.value}")
                newsListViewModel.prePopulating()
                newsListViewModel.getDataFromDatabase(newsListViewModel.currentCategory.value ?: "all")
                populateData()
                newsRecyclerView.scrollToPosition(0)
                newsListViewModel.onCompleteCategoryClicked()
            }
        }

        newsListViewModel.showErrorToast.observe(viewLifecycleOwner) {
            if(it) {
                Log.i("NewsListFragment", "showErrorToast")
                val toast = Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT)
                toast.show()
                newsListViewModel.onCompletedShowErrorToast()
            }
        }

        newsListViewModel.isLoading.observe(viewLifecycleOwner) {
            if(it) {
                binding.loadingIndicator.visibility = View.VISIBLE
            } else {
                binding.loadingIndicator.visibility = View.GONE
            }
        }

        newsListViewModel.isLoadingWeather.observe(viewLifecycleOwner) {
            if(it) {
                binding.weatherBar.visibility = View.VISIBLE
            } else {
                binding.weatherBar.visibility = View.GONE
            }
        }

        newsListViewModel.isLocationPresent.observe(viewLifecycleOwner) {
            if(it) {
                binding.weatherBar.visibility = View.GONE
                Picasso.get().load(R.drawable.cloud_off_foreground).into(binding.tempImage)
                newsListViewModel.isLocationPresent.value = false
            }
        }

        newsListViewModel.weatherData.observe(viewLifecycleOwner) { weatherData ->
            // Update UI with weather data
            weatherData?.let {
                binding.tempText.text = "${it.temperature}°C"
                Picasso.get().load(newsListViewModel.getWeatherImage(newsListViewModel.getWeatherCondition(it))).into(binding.tempImage)
            }
        }

        newsListViewModel.newsItemClicked.observe(viewLifecycleOwner) {
            if(it) {
                val action = NewsListFragmentDirections.actionNewsListFragmentToNewsDetailsFragment(
                    newsListViewModel.clickedCurrentNews.value?.title!!,
                    newsListViewModel.clickedCurrentNews.value?.content!!,
                    newsListViewModel.clickedCurrentNews.value?.imageUrl!!,
                    newsListViewModel.clickedCurrentNews.value?.readMoreUrl!!,
                )
                findNavController().navigate(action)
                newsListViewModel.onCompletedNavigation()
            }
        }

        newsListViewModel.newsData.observe(viewLifecycleOwner) {
            newsItemAdapter.submitList(it)
        }

        newsListViewModel.showNoNewsToast.observe(viewLifecycleOwner) {
            if(it) {
                Log.i("NewsListFragment", "showNoNewsToast")
                val toast = Toast.makeText(context, "No news available", Toast.LENGTH_SHORT)
                toast.show()
                newsListViewModel.onCompletedShowNoNewsToast()
            }
        }

        newsListViewModel.shareNews.observe(viewLifecycleOwner) {
            if(it) {
                startActivity(getShareIntent())
                newsListViewModel.onCompletedShareNews()
            }
        }

        val slideDownAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down)
        val slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)

        val orientation = resources.configuration.orientation
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        var isScrollingDown = false


        newsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

//                val orientation = resources.configuration.orientation
//                val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE

                // Check if scrolling down and app is in landscape mode
                if (isLandscape && dy > 0) {
                    // Landscape mode and scrolling down
                    if (!isScrollingDown) {
                        isScrollingDown = true
                        //hide the categoryRecyclerView smoothly
                        categoryRecyclerView.startAnimation(slideUpAnimation)
                        categoryRecyclerView.visibility = View.GONE
                    }
                } else if(isLandscape && dy <= 0) {
                    isScrollingDown = false
                    //show the categoryRecyclerView smoothly
                    categoryRecyclerView.startAnimation(slideDownAnimation)
                    categoryRecyclerView.visibility = View.VISIBLE
                }
                if (!newsListViewModel.isLoading.value!! && visibleItemCount + firstVisibleItemPosition >= totalItemCount
                    && firstVisibleItemPosition >= 0
                ) {
                    newsListViewModel.loadMoreData(newsListViewModel.currentCategory.value ?: "all")
                }
            }
        })

        val searchView = binding.searchView

        val searchViewWidth = searchView.layoutParams.width

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // SearchView gained focus, collapse temperature details
                binding.tempText.visibility = View.GONE
                // match parent width
                searchView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                // SearchView lost focus, bring back temperature details
                binding.tempText.visibility = View.VISIBLE
                // wrap content width
                searchView.layoutParams.width = searchViewWidth
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Perform search when user submits query
                query?.let {
                    newsListViewModel.searchNews(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })



        return binding.root
    }

    private fun getShareIntent() : Intent {
        return ShareCompat.IntentBuilder.from(requireActivity())
            .setText("Check out this article: " + newsListViewModel.shareNewsTitle.value + "\n" + newsListViewModel.shareNewsUrl.value)
            .setType("text/plain")
            .intent
    }

    private fun populateData() {
        val adapter = newsRecyclerView.adapter as NewsListAdapter
        if(newsListViewModel.newsData.value != null)
            adapter.submitList(newsListViewModel.newsData.value)
    }

    private fun requestLocationPermission() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, fetch weather data
                Log.i("NewsListFragment","Permission granted.")
                newsListViewModel.getCurrentLocationAndFetchWeather()
            } else {
                // Permission denied, handle accordingly
                Log.i("NewsListFragment", "Location permission denied")
            }
        }
        locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }


    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 123
    }

    override fun onImageClicked(imageUrl: String) {
        val dialog = EnlargeImageDialog(requireContext(), imageUrl)
        dialog.show()
    }
}