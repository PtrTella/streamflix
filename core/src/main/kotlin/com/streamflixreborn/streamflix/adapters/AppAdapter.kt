package com.streamflixreborn.streamflix.adapters

object AppAdapter {
    interface Item {
        var itemType: Type
            get() = Type.CATEGORY_MOBILE_ITEM
            set(_) {}
    }

    enum class Type {
        CATEGORY_MOBILE_ITEM,
        CATEGORY_TV_ITEM,
        CATEGORY_MOBILE_SWIPER,
        CATEGORY_TV_SWIPER,
        EPISODE_MOBILE_ITEM,
        EPISODE_TV_ITEM,
        EPISODE_CONTINUE_WATCHING_MOBILE_ITEM,
        EPISODE_CONTINUE_WATCHING_TV_ITEM,
        GENRE_MOBILE_ITEM,
        GENRE_TV_ITEM,
        MOVIE_MOBILE_ITEM,
        MOVIE_TV_ITEM,
        MOVIE_SWIPER_MOBILE_ITEM,
        MOVIE_SWIPER_TV_ITEM,
        PEOPLE_MOBILE_ITEM,
        PEOPLE_TV_ITEM,
        PROVIDER_MOBILE_ITEM,
        PROVIDER_TV_ITEM,
        SEASON_MOBILE_ITEM,
        SEASON_TV_ITEM,
        TV_SHOW_MOBILE_ITEM,
        TV_SHOW_TV_ITEM,
        TV_SHOW_SWIPER_MOBILE_ITEM,
        TV_SHOW_SWIPER_TV_ITEM
    }
}
