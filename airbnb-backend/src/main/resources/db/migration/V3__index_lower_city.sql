-- Search compares lower(l.city) = lower(:city) (ListingRepository), so "goa" finds "Goa". A plain index on
-- city cannot serve that comparison: the database would read every listing. An index on the expression
-- lower(city) can, because the query uses exactly the same expression.

drop index idx_listing_city;
create index idx_listing_lower_city on listing (lower(city));
