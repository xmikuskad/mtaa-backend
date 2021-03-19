--
-- PostgreSQL database dump
--

-- Dumped from database version 13.2
-- Dumped by pg_dump version 13.2

-- Started on 2021-03-19 17:09:21

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 6 (class 2615 OID 16395)
-- Name: mtaa; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA mtaa;


ALTER SCHEMA mtaa OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 206 (class 1259 OID 16442)
-- Name: brands; Type: TABLE; Schema: mtaa; Owner: postgres
--

CREATE TABLE mtaa.brands (
    id integer NOT NULL,
    name character varying NOT NULL
);


ALTER TABLE mtaa.brands OWNER TO postgres;

--
-- TOC entry 205 (class 1259 OID 16434)
-- Name: categories; Type: TABLE; Schema: mtaa; Owner: postgres
--

CREATE TABLE mtaa.categories (
    id integer NOT NULL,
    name character varying NOT NULL
);


ALTER TABLE mtaa.categories OWNER TO postgres;

--
-- TOC entry 207 (class 1259 OID 16450)
-- Name: categoriesbrands; Type: TABLE; Schema: mtaa; Owner: postgres
--

CREATE TABLE mtaa.categoriesbrands (
    id integer NOT NULL,
    brand_id integer NOT NULL,
    category_id integer NOT NULL
);


ALTER TABLE mtaa.categoriesbrands OWNER TO postgres;

--
-- TOC entry 210 (class 1259 OID 16503)
-- Name: photos; Type: TABLE; Schema: mtaa; Owner: postgres
--

CREATE TABLE mtaa.photos (
    id integer NOT NULL,
    review_id integer NOT NULL,
    source character varying NOT NULL
);


ALTER TABLE mtaa.photos OWNER TO postgres;

--
-- TOC entry 208 (class 1259 OID 16470)
-- Name: products; Type: TABLE; Schema: mtaa; Owner: postgres
--

CREATE TABLE mtaa.products (
    id integer NOT NULL,
    price integer NOT NULL,
    brand_id integer NOT NULL,
    category_id integer NOT NULL,
    score integer NOT NULL
);


ALTER TABLE mtaa.products OWNER TO postgres;

--
-- TOC entry 212 (class 1259 OID 16566)
-- Name: reviewattributes; Type: TABLE; Schema: mtaa; Owner: postgres
--

CREATE TABLE mtaa.reviewattributes (
    id integer NOT NULL,
    is_positive boolean NOT NULL,
    "review_ID" integer NOT NULL,
    text character varying NOT NULL
);


ALTER TABLE mtaa.reviewattributes OWNER TO postgres;

--
-- TOC entry 209 (class 1259 OID 16485)
-- Name: reviews; Type: TABLE; Schema: mtaa; Owner: postgres
--

CREATE TABLE mtaa.reviews (
    id integer NOT NULL,
    text character varying NOT NULL,
    product_id integer NOT NULL,
    user_id integer NOT NULL,
    score integer NOT NULL,
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE mtaa.reviews OWNER TO postgres;

--
-- TOC entry 211 (class 1259 OID 16551)
-- Name: reviewvotes; Type: TABLE; Schema: mtaa; Owner: postgres
--

CREATE TABLE mtaa.reviewvotes (
    id integer NOT NULL,
    is_positive boolean NOT NULL,
    user_id integer NOT NULL,
    review_id integer NOT NULL
);


ALTER TABLE mtaa.reviewvotes OWNER TO postgres;

--
-- TOC entry 204 (class 1259 OID 16426)
-- Name: users; Type: TABLE; Schema: mtaa; Owner: postgres
--

CREATE TABLE mtaa.users (
    id integer NOT NULL,
    password character varying NOT NULL,
    email character varying NOT NULL,
    trust_score integer NOT NULL
);


ALTER TABLE mtaa.users OWNER TO postgres;

--
-- TOC entry 203 (class 1259 OID 16424)
-- Name: users_id_seq; Type: SEQUENCE; Schema: mtaa; Owner: postgres
--

ALTER TABLE mtaa.users ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME mtaa.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 3055 (class 0 OID 16442)
-- Dependencies: 206
-- Data for Name: brands; Type: TABLE DATA; Schema: mtaa; Owner: postgres
--

COPY mtaa.brands (id, name) FROM stdin;
\.


--
-- TOC entry 3054 (class 0 OID 16434)
-- Dependencies: 205
-- Data for Name: categories; Type: TABLE DATA; Schema: mtaa; Owner: postgres
--

COPY mtaa.categories (id, name) FROM stdin;
\.


--
-- TOC entry 3056 (class 0 OID 16450)
-- Dependencies: 207
-- Data for Name: categoriesbrands; Type: TABLE DATA; Schema: mtaa; Owner: postgres
--

COPY mtaa.categoriesbrands (id, brand_id, category_id) FROM stdin;
\.


--
-- TOC entry 3059 (class 0 OID 16503)
-- Dependencies: 210
-- Data for Name: photos; Type: TABLE DATA; Schema: mtaa; Owner: postgres
--

COPY mtaa.photos (id, review_id, source) FROM stdin;
\.


--
-- TOC entry 3057 (class 0 OID 16470)
-- Dependencies: 208
-- Data for Name: products; Type: TABLE DATA; Schema: mtaa; Owner: postgres
--

COPY mtaa.products (id, price, brand_id, category_id, score) FROM stdin;
\.


--
-- TOC entry 3061 (class 0 OID 16566)
-- Dependencies: 212
-- Data for Name: reviewattributes; Type: TABLE DATA; Schema: mtaa; Owner: postgres
--

COPY mtaa.reviewattributes (id, is_positive, "review_ID", text) FROM stdin;
\.


--
-- TOC entry 3058 (class 0 OID 16485)
-- Dependencies: 209
-- Data for Name: reviews; Type: TABLE DATA; Schema: mtaa; Owner: postgres
--

COPY mtaa.reviews (id, text, product_id, user_id, score, created_at) FROM stdin;
\.


--
-- TOC entry 3060 (class 0 OID 16551)
-- Dependencies: 211
-- Data for Name: reviewvotes; Type: TABLE DATA; Schema: mtaa; Owner: postgres
--

COPY mtaa.reviewvotes (id, is_positive, user_id, review_id) FROM stdin;
\.


--
-- TOC entry 3053 (class 0 OID 16426)
-- Dependencies: 204
-- Data for Name: users; Type: TABLE DATA; Schema: mtaa; Owner: postgres
--

COPY mtaa.users (id, password, email, trust_score) FROM stdin;
\.


--
-- TOC entry 3067 (class 0 OID 0)
-- Dependencies: 203
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: mtaa; Owner: postgres
--

SELECT pg_catalog.setval('mtaa.users_id_seq', 1, false);


--
-- TOC entry 2899 (class 2606 OID 16449)
-- Name: brands brands_pkey; Type: CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.brands
    ADD CONSTRAINT brands_pkey PRIMARY KEY (id);


--
-- TOC entry 2897 (class 2606 OID 16441)
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);


--
-- TOC entry 2901 (class 2606 OID 16454)
-- Name: categoriesbrands categoriesbrands_pkey; Type: CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.categoriesbrands
    ADD CONSTRAINT categoriesbrands_pkey PRIMARY KEY (id);


--
-- TOC entry 2907 (class 2606 OID 16510)
-- Name: photos photos_pkey; Type: CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.photos
    ADD CONSTRAINT photos_pkey PRIMARY KEY (id);


--
-- TOC entry 2903 (class 2606 OID 16474)
-- Name: products products_pkey; Type: CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- TOC entry 2911 (class 2606 OID 16573)
-- Name: reviewattributes reviewattributes_pkey; Type: CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.reviewattributes
    ADD CONSTRAINT reviewattributes_pkey PRIMARY KEY (id);


--
-- TOC entry 2905 (class 2606 OID 16492)
-- Name: reviews reviews_pkey; Type: CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (id);


--
-- TOC entry 2909 (class 2606 OID 16555)
-- Name: reviewvotes reviewvotes_pkey; Type: CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.reviewvotes
    ADD CONSTRAINT reviewvotes_pkey PRIMARY KEY (id);


--
-- TOC entry 2895 (class 2606 OID 16433)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 2913 (class 2606 OID 16526)
-- Name: categoriesbrands brand_ID; Type: FK CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.categoriesbrands
    ADD CONSTRAINT "brand_ID" FOREIGN KEY (brand_id) REFERENCES mtaa.brands(id) NOT VALID;


--
-- TOC entry 2915 (class 2606 OID 16536)
-- Name: products brand_ID; Type: FK CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.products
    ADD CONSTRAINT "brand_ID" FOREIGN KEY (brand_id) REFERENCES mtaa.brands(id) NOT VALID;


--
-- TOC entry 2912 (class 2606 OID 16521)
-- Name: categoriesbrands category_ID; Type: FK CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.categoriesbrands
    ADD CONSTRAINT "category_ID" FOREIGN KEY (category_id) REFERENCES mtaa.categories(id) NOT VALID;


--
-- TOC entry 2914 (class 2606 OID 16531)
-- Name: products category_ID; Type: FK CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.products
    ADD CONSTRAINT "category_ID" FOREIGN KEY (category_id) REFERENCES mtaa.categories(id) NOT VALID;


--
-- TOC entry 2917 (class 2606 OID 16546)
-- Name: reviews product_ID; Type: FK CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.reviews
    ADD CONSTRAINT "product_ID" FOREIGN KEY (product_id) REFERENCES mtaa.products(id) NOT VALID;


--
-- TOC entry 2918 (class 2606 OID 16511)
-- Name: photos review_ID; Type: FK CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.photos
    ADD CONSTRAINT "review_ID" FOREIGN KEY (review_id) REFERENCES mtaa.reviews(id);


--
-- TOC entry 2919 (class 2606 OID 16556)
-- Name: reviewvotes review_ID; Type: FK CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.reviewvotes
    ADD CONSTRAINT "review_ID" FOREIGN KEY (review_id) REFERENCES mtaa.reviews(id);


--
-- TOC entry 2921 (class 2606 OID 16574)
-- Name: reviewattributes review_ID; Type: FK CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.reviewattributes
    ADD CONSTRAINT "review_ID" FOREIGN KEY ("review_ID") REFERENCES mtaa.reviews(id);


--
-- TOC entry 2916 (class 2606 OID 16541)
-- Name: reviews user_ID; Type: FK CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.reviews
    ADD CONSTRAINT "user_ID" FOREIGN KEY (user_id) REFERENCES mtaa.users(id) NOT VALID;


--
-- TOC entry 2920 (class 2606 OID 16561)
-- Name: reviewvotes user_ID; Type: FK CONSTRAINT; Schema: mtaa; Owner: postgres
--

ALTER TABLE ONLY mtaa.reviewvotes
    ADD CONSTRAINT "user_ID" FOREIGN KEY (user_id) REFERENCES mtaa.users(id);


-- Completed on 2021-03-19 17:09:21

--
-- PostgreSQL database dump complete
--

