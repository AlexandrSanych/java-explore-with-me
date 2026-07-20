package ru.practicum.main.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.response.LocationDto;
import ru.practicum.main.model.Location;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationMapper {

    public static Location toLocation(LocationDto dto) {
        if (dto == null) {
            return null;
        }
        return Location.builder()
                .lat(dto.getLat())
                .lon(dto.getLon())
                .build();
    }

    public static LocationDto toLocationDto(Location location) {
        if (location == null) {
            return null;
        }
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }
}