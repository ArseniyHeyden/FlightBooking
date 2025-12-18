package com.example.flightbooking.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.flightbooking.R
import com.example.flightbooking.data.entity.Flight
import com.example.flightbooking.data.entity.FlightWithDetails

class FlightAdapter(
    private val onFlightClick: (Flight) -> Unit,
    private val onFavoriteClick: (Flight) -> Unit,
    private val getPriceText: ((Flight) -> String)? = null,
    private val allFlights: List<FlightWithDetails> = emptyList() // Для определения лучших предложений
) : ListAdapter<FlightWithDetails, FlightAdapter.FlightViewHolder>(FlightDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlightViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_flight, parent, false)
        return FlightViewHolder(view, allFlights.map { it.flight })
    }

    override fun onBindViewHolder(holder: FlightViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FlightViewHolder(
        itemView: View,
        private val allFlightsList: List<Flight>
    ) : RecyclerView.ViewHolder(itemView) {
        private val fromCityText: TextView = itemView.findViewById(R.id.from_city_text)
        private val fromAirportText: TextView = itemView.findViewById(R.id.from_airport_text)
        private val toCityText: TextView = itemView.findViewById(R.id.to_city_text)
        private val toAirportText: TextView = itemView.findViewById(R.id.to_airport_text)
        private val departureTimeText: TextView = itemView.findViewById(R.id.departure_time_text)
        private val arrivalTimeText: TextView = itemView.findViewById(R.id.arrival_time_text)
        private val durationText: TextView = itemView.findViewById(R.id.duration_text)
        private val transferInfoText: TextView = itemView.findViewById(R.id.transfer_info_text)
        private val airlineText: TextView = itemView.findViewById(R.id.airline_text)
        private val baggageIcon: ImageView = itemView.findViewById(R.id.baggage_icon)
        private val baggageText: TextView = itemView.findViewById(R.id.baggage_text)
        private val priceText: TextView = itemView.findViewById(R.id.price_text)
        private val favoriteButton: ImageButton = itemView.findViewById(R.id.favorite_button)
        private val badgesContainer: LinearLayout = itemView.findViewById(R.id.badges_container)
        private val bestPriceBadge: TextView = itemView.findViewById(R.id.best_price_badge)
        private val fastestBadge: TextView = itemView.findViewById(R.id.fastest_badge)
        private val convenientBadge: TextView = itemView.findViewById(R.id.convenient_badge)

        fun bind(flightWithDetails: FlightWithDetails) {
            val flight = flightWithDetails.flight

            // Города и аэропорты
            fromCityText.text = flight.fromCity
            fromAirportText.text = flight.fromAirport.ifBlank { getDefaultAirport(flight.fromCity) }
            toCityText.text = flight.toCity
            toAirportText.text = flight.toAirport.ifBlank { getDefaultAirport(flight.toCity) }

            // Время
            departureTimeText.text = flight.departureTime
            arrivalTimeText.text = flight.arrivalTime
            durationText.text = formatDuration(flight.duration)

            // Пересадка
            if (flight.hasTransfers && flight.transferCity != null) {
                transferInfoText.text = "Пересадка в ${flight.transferCity}"
                transferInfoText.visibility = View.VISIBLE
            } else {
                transferInfoText.visibility = View.GONE
            }

            // Авиакомпания
            airlineText.text = flight.airline

            // Багаж
            if (flight.includesBaggage) {
                baggageIcon.visibility = View.VISIBLE
                baggageText.text = "Багаж"
                baggageText.visibility = View.VISIBLE
            } else {
                baggageIcon.visibility = View.GONE
                baggageText.text = "Без багажа"
                baggageText.visibility = View.VISIBLE
            }

            // Цена
            val priceDisplay = if (flight.isHotDeal && flight.hotDealDiscount > 0) {
                val discountedPrice = flight.basePrice * (1.0 - flight.hotDealDiscount / 100.0)
                "🔥 ${String.format("%.0f", discountedPrice)} ₽"
            } else {
                getPriceText?.invoke(flight) ?: "${String.format("%.0f", flight.basePrice)} ₽"
            }
            priceText.text = priceDisplay

            // Избранное
            favoriteButton.setImageResource(
                if (flightWithDetails.isFavorite) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite_border
            )

            // Метки лучших предложений
            updateBadges(flight)

            itemView.setOnClickListener { onFlightClick(flight) }
            favoriteButton.setOnClickListener { onFavoriteClick(flight) }
        }

        private fun updateBadges(flight: Flight) {
            val flightsForRoute = allFlightsList.filter {
                it.fromCity == flight.fromCity && it.toCity == flight.toCity
            }

            if (flightsForRoute.isEmpty()) {
                badgesContainer.visibility = View.GONE
                return
            }

            val cheapestFlight = flightsForRoute.minByOrNull { it.basePrice }
            val fastestFlight = flightsForRoute.minByOrNull { it.duration }
            val mostConvenientFlight = flightsForRoute
                .filter { !it.hasTransfers && it.includesBaggage }
                .minByOrNull { it.duration }

            bestPriceBadge.visibility = if (cheapestFlight?.id == flight.id) View.VISIBLE else View.GONE
            fastestBadge.visibility = if (fastestFlight?.id == flight.id) View.VISIBLE else View.GONE
            convenientBadge.visibility = if (mostConvenientFlight?.id == flight.id && !flight.hasTransfers && flight.includesBaggage) View.VISIBLE else View.GONE

            badgesContainer.visibility = if (
                bestPriceBadge.visibility == View.VISIBLE ||
                fastestBadge.visibility == View.VISIBLE ||
                convenientBadge.visibility == View.VISIBLE
            ) View.VISIBLE else View.GONE
        }

        private fun getDefaultAirport(city: String): String {
            return when (city) {
                "Москва" -> "Шереметьево"
                "Санкт-Петербург" -> "Пулково"
                "Казань" -> "Казань"
                "Сочи" -> "Адлер"
                "Екатеринбург" -> "Кольцово"
                "Новосибирск" -> "Толмачёво"
                else -> ""
            }
        }

        private fun formatDuration(minutes: Int): String {
            val hours = minutes / 60
            val mins = minutes % 60
            return "${hours}ч ${mins}м"
        }
    }

    class FlightDiffCallback : DiffUtil.ItemCallback<FlightWithDetails>() {
        override fun areItemsTheSame(
            oldItem: FlightWithDetails,
            newItem: FlightWithDetails
        ): Boolean {
            return oldItem.flight.id == newItem.flight.id
        }

        override fun areContentsTheSame(
            oldItem: FlightWithDetails,
            newItem: FlightWithDetails
        ): Boolean {
            return oldItem == newItem
        }
    }
}