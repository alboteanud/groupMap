package com.craiovadata.groupmap.utils_

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.model.User
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator
import java.lang.Exception
import java.util.*
import kotlin.math.min

class MarkerRenderer(private val context: Context, map: GoogleMap?, clusterManager: ClusterManager<User>?) :
    DefaultClusterRenderer<User>(context, map, clusterManager) {
    private val mIconGenerator: IconGenerator = IconGenerator(context)
    private val mClusterIconGenerator: IconGenerator = IconGenerator(context)
    private val mImageView: ImageView
    private val mClusterImageView: ImageView
    private val mDimension: Int
//    private val map: GoogleMap

    init {
        val multiProfile = LayoutInflater.from(context).inflate(R.layout.multi_profile, null)
        mClusterIconGenerator.setContentView(multiProfile)
        mClusterImageView = multiProfile.findViewById<View>(R.id.image) as ImageView

        mImageView = ImageView(context)
        mDimension = context.resources.getDimension(R.dimen.custom_profile_image).toInt()
        mImageView.layoutParams = ViewGroup.LayoutParams(mDimension, mDimension)
        val padding = context.resources.getDimension(R.dimen.custom_profile_padding).toInt()
        mImageView.setPadding(padding, padding, padding, padding)
        mIconGenerator.setContentView(mImageView)
//        this.map = map
    }

    override fun onBeforeClusterItemRendered(user: User?, markerOptions: MarkerOptions) {
        // Draw a single user.
        // Set the info window to show their name.
//        mImageView.setImageResource(R.drawable.ic_face);
//        val icon = mIconGenerator.makeIcon()
//        val name = user?.name ?: "?"
//        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title(name)
    }

    override fun onClusterItemRendered(clusterItem: User, marker: Marker) {
        super.onClusterItemRendered(clusterItem, marker)
//        if (clusterItem == null || marker == null) return
        marker.tag = TAG
        val requestOptions = RequestOptions()
            .centerCrop()
            .error(R.mipmap.ic_person)
            .placeholder(R.mipmap.ic_person)

        try {
            Glide.with(context)
                .load(clusterItem.photoUrl)
                .apply(requestOptions)
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(drawable: Drawable, transition: Transition<in Drawable>?) {
                        drawIcon(drawable)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        drawIcon(placeholder)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        drawIcon(errorDrawable)
                    }

                    private fun drawIcon(drawable: Drawable?) {
                        if (drawable == null) return
                        mImageView.setImageDrawable(drawable)
                        val icon = mIconGenerator.makeIcon()
                        val iconDescriptor = BitmapDescriptorFactory.fromBitmap(icon)
//                        marker.title = clusterItem.name
                        if (marker.tag != null) marker.setIcon(iconDescriptor)
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onClusterRendered(cluster: Cluster<User>, marker: Marker) {
        super.onClusterRendered(cluster, marker)
//        if (cluster == null || marker == null) return
        marker.tag = TAG
        // Draw multiple people.
        // Note: this method runs on the UI thread. Don't spend too much time in here (like in this example).
        val profilePhotos = ArrayList<Drawable>(min(4, cluster.size))
        val width = mDimension
        val height = mDimension
        cluster.items.forEachIndexed { index, person ->
            // Draw 4 at most.
            if (index == 4) return@forEachIndexed

            val requestOptions = RequestOptions()
                .centerCrop()
                .error(R.mipmap.ic_person)
                .placeholder(R.mipmap.ic_person)
            try {
                Glide.with(context)
//                .asBitmap()
                    .load(person.photoUrl)
                    .apply(requestOptions)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(drawable: Drawable, transition: Transition<in Drawable>?) {
                            drawCluster(drawable)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            drawCluster(placeholder)
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            drawCluster(errorDrawable)
                        }

                        private fun drawCluster(drawable: Drawable?) {
                            if (drawable == null) return
                            drawable.setBounds(0, 0, width, height)
                            profilePhotos.add(drawable)

                            if (index == 3 || index == (cluster.items.size - 1)) {
                                val multiDrawable = MultiDrawable(profilePhotos)
                                multiDrawable.setBounds(0, 0, width, height)
                                mClusterImageView.setImageDrawable(multiDrawable)
                                val icon = mClusterIconGenerator.makeIcon(cluster.size.toString())
                                val iconDescriptor = BitmapDescriptorFactory.fromBitmap(icon)
                                if (marker.tag != null) marker.setIcon(iconDescriptor)
                            }
                        }
                    })
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    override fun onBeforeClusterRendered(cluster: Cluster<User>, markerOptions: MarkerOptions) {

        // Draw multiple people.
        // Note: this method runs on the UI thread. Don't spend too much time in here (like in this example).

//        val profilePhotos = ArrayList<Drawable>(Math.min(4, cluster.size))
//        val width = mDimension
//        val height = mDimension
//
//        for (person in cluster.items) {
//            // Draw 4 at most.
//            if (profilePhotos.size == 4) break
//            val drawable = context.getDrawable(R.drawable.ic_satelite) ?: return
//            drawable.setBounds(0, 0, width, height)
//            profilePhotos.add(drawable)
//
//            val multiDrawable = MultiDrawable(profilePhotos)
//            multiDrawable.setBounds(0, 0, width, height)
//
//            mClusterImageView.setImageDrawable(multiDrawable)
//            val icon = mClusterIconGenerator.makeIcon(cluster.size.toString())
//            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon))
//        }

    }

    override fun shouldRenderAsCluster(cluster: Cluster<User>): Boolean {
//        return super.shouldRenderAsCluster(cluster)
        // Always render clusters.
        return cluster.size > 1
    }

    companion object{
        const val TAG = "_"
    }


}
