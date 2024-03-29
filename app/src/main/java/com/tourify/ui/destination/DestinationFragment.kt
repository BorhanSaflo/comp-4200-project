package com.tourify.ui.destination

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.tourify.ImageAdapter
import com.tourify.ImageFetcher
import com.tourify.R
import com.tourify.models.Review
import com.tourify.utils.ApiResponse
import com.tourify.viewmodels.CoroutinesErrorHandler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DestinationFragment : Fragment() {
    private val viewModel: DestinationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_destination, container, false)
        // Set up the Toolbar to act as the ActionBar
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        (activity as? AppCompatActivity)?.let { activity ->
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity.supportActionBar?.setDisplayShowHomeEnabled(true)
            // Set a click listener on the navigation icon (back button)
            toolbar.setNavigationOnClickListener {
                activity.onBackPressed()
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val id = requireArguments().getInt("id")
        val destinationDescription: TextView = view.findViewById(R.id.destination_description_text_view)
        val destinationLocation = view.findViewById<TextView>(R.id.destination_location_text_view)
        val likeView = view.findViewById<TextView>(R.id.like_text_view)
        val dislikeView = view.findViewById<TextView>(R.id.dislike_text_view)
        val viewsView = view.findViewById<TextView>(R.id.view_text_view)
        val savedView = view.findViewById<ImageView>(R.id.save_button)

        viewModel.destinationResponse.observe(viewLifecycleOwner) {
            when(it) {
                is ApiResponse.Success -> {
                    val destination = it.data
                    destinationLocation.text = destination.name + ", " + destination.country
                    destinationDescription.text = destination.description
                    likeView.text = destination.likes.toString()
                    dislikeView.text = destination.dislikes.toString()
                    viewsView.text = destination.views.toString()

                    fun displayImages(urls: List<String>){
                        val imageProgressBar = view.findViewById<View>(R.id.image_progress_bar)
                        imageProgressBar.visibility = View.VISIBLE

                        val images = mutableListOf<Bitmap>()
                        val imageFetchListener = ImageFetcher.ImageFetchListener { bitmap ->
                            if (bitmap == null) {
                                Toast.makeText(requireContext(), "Failed to load images", Toast.LENGTH_SHORT).show()
                                return@ImageFetchListener
                            }
                            images.add(bitmap)
                            if (images.size == urls.size) {
                                val destinationViewPager = view.findViewById<ViewPager2>(R.id.destination_view_pager)
                                destinationViewPager.adapter = ImageAdapter(images)
                                imageProgressBar.visibility = View.INVISIBLE
                            }
                        }
                        ImageFetcher.fetchImages(urls, imageFetchListener)
                    }

                    displayImages(destination.images)
                }
                else -> {}
            }

            // Reviews
            val reviewLayout: ViewGroup = view.findViewById(R.id.review_layout)
            when(it) {
                is ApiResponse.Success -> addReview(it.data.reviewsQuery, reviewLayout)
                else -> {}
            }
        }

        viewModel.getDestination(id, object: CoroutinesErrorHandler {
            override fun onError(message: String) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        })

        var currentVote = 0
        var oldVote = 0
        val likeButton = view.findViewById<ImageView>(R.id.like_image_view)
        val dislikeButton = view.findViewById<ImageView>(R.id.dislike_image_view)

        likeButton.setOnClickListener {
            if (currentVote == 1) {
                currentVote = 0
                updateLikeRating(currentVote, oldVote)
            } else {
                currentVote = 1
                updateLikeRating(currentVote, oldVote)
            }
            oldVote = currentVote
        }

        dislikeButton.setOnClickListener {
            if (currentVote == 2) {
                currentVote = 0
                updateLikeRating(currentVote, oldVote)
            } else {
                currentVote = 2
                updateLikeRating(currentVote, oldVote)
            }
            oldVote = currentVote
        }

        var isSaved = false
        viewModel.savedDestinationsResponse.observe(viewLifecycleOwner) {
            when(it) {
                is ApiResponse.Success -> {
                    val savedDestinations = it.data
                    for (destination in savedDestinations) {
                        if (destination.id.toInt() == id) {
                            updateBookmark(true)
                            isSaved = true
                            break
                        }
                    }
                }
                else -> {}
            }
        }

        viewModel.getSavedDestinations(object: CoroutinesErrorHandler {
            override fun onError(message: String) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        })

        savedView.setOnClickListener {
            isSaved = !isSaved
            updateBookmark(isSaved)

            viewModel.saveDestination(id, object: CoroutinesErrorHandler {
                override fun onError(message: String) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun updateLikeRating(currentVote: Int, oldVote: Int) {
        val likeImage = view?.findViewById<ImageView>(R.id.like_image_view)
        val likeText = view?.findViewById<TextView>(R.id.like_text_view)
        val dislikeImage = view?.findViewById<ImageView>(R.id.dislike_image_view)
        val dislikeText = view?.findViewById<TextView>(R.id.dislike_text_view)
        val newLikeImage: Int
        val newDislikeImage: Int

        if(currentVote == 1) {
            likeText?.text = (likeText?.text.toString().toInt() + 1).toString()
            newLikeImage = R.drawable.thumbs_up_green
            newDislikeImage = R.drawable.thumbs_down
            if (oldVote == 2)
                dislikeText?.text = (dislikeText?.text.toString().toInt() - 1).toString()
        } else if (currentVote == 2){
            dislikeText?.text = (dislikeText?.text.toString().toInt() + 1).toString()
            newLikeImage = R.drawable.thumbs_up
            newDislikeImage = R.drawable.thumbs_down_red
            if (oldVote == 1)
                likeText?.text = (likeText?.text.toString().toInt() - 1).toString()
        } else {
            newLikeImage = R.drawable.thumbs_up
            newDislikeImage = R.drawable.thumbs_down
            if(oldVote == 1)
                likeText?.text = (likeText?.text.toString().toInt() - 1).toString()
            else if(oldVote == 2)
                dislikeText?.text = (dislikeText?.text.toString().toInt() - 1).toString()
        }

        likeImage?.setImageDrawable(ResourcesCompat.getDrawable(resources, newLikeImage, null))
        dislikeImage?.setImageDrawable(ResourcesCompat.getDrawable(resources, newDislikeImage, null))
    }

    private fun updateBookmark(isSaved: Boolean) {
        val savedView = view?.findViewById<ImageView>(R.id.save_button)
        val newImage = if (isSaved) R.drawable.bookmark_checked else R.drawable.bookmark_unchecked
        savedView?.setImageDrawable(ResourcesCompat.getDrawable(resources, newImage, null))
    }

    private fun addReview(reviews: List<Review>, layout: ViewGroup) {
        layout.removeAllViews()
        for (review in reviews) {
            val placeLayout = LayoutInflater.from(context).inflate(R.layout.item_review, layout, false)
            val imageView = placeLayout.findViewById<ImageView>(R.id.review_image)
            val nameView = placeLayout.findViewById<TextView>(R.id.review_name)
            val bodyView = placeLayout.findViewById<TextView>(R.id.review_body)
            val timestampView = placeLayout.findViewById<TextView>(R.id.review_timestamp)

            imageView.setImageResource(R.drawable.user)
            nameView.text = review.user.name
            bodyView.text = review.comment

            val relativeTime = review.timestamp?.let { viewModel.getRelativeTime(it) }
            timestampView.text = relativeTime

            layout.addView(placeLayout)
        }
    }
}