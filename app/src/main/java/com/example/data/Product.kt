package com.example.data

data class Product(
    val id: String,
    val title: String,
    val minPrice: Double,
    val maxPrice: Double,
    val category: String, // Mode, Sport, Maison, Accessoires
    val variants: List<String>,
    val imageUrl: String,
    val description: String
)

object ProductCatalog {
    val items = listOf(
        Product(
            id = "t-shirt-blanc",
            title = "T-shirt blanc personnalisation",
            minPrice = 22.0,
            maxPrice = 28.0,
            category = "Mode",
            variants = listOf("XS", "S", "M", "L", "XL", "XXL"),
            imageUrl = "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500&auto=format&fit=crop&q=60",
            description = "T-shirt blanc de qualité premium. Fabriqué en coton 100% biologique, idéal pour toutes vos créations de logos et textes personnalisés."
        ),
        Product(
            id = "t-shirt-noir",
            title = "T-shirt noir personnalisation",
            minPrice = 22.0,
            maxPrice = 28.0,
            category = "Mode",
            variants = listOf("XS", "S", "M", "L", "XL", "XXL"),
            imageUrl = "https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=500&auto=format&fit=crop&q=60",
            description = "T-shirt noir coupe moderne et tissu dense. Résiste parfaitement aux lavages successifs et sublime vos designs aux teintes claires."
        ),
        Product(
            id = "hoodie",
            title = "Sweat à capuche (hoodie)",
            minPrice = 45.0,
            maxPrice = 55.0,
            category = "Mode",
            variants = listOf("S", "M", "L", "XL"),
            imageUrl = "https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=500&auto=format&fit=crop&q=60",
            description = "Hoodie ultra-confortable doublé polaire. Parfait pour un style streetwear avec impression broderie ou flocage de votre motif fétiche."
        ),
        Product(
            id = "crop-top",
            title = "Crop top personnalisable",
            minPrice = 20.0,
            maxPrice = 26.0,
            category = "Mode",
            variants = listOf("XS", "S", "M", "L"),
            imageUrl = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=500&auto=format&fit=crop&q=60",
            description = "Crop top en jersey de coton stretch, idéal pour les tenues légères d'été ou les looks sportswear affirmés."
        ),
        Product(
            id = "legging",
            title = "Legging de sport",
            minPrice = 30.0,
            maxPrice = 38.0,
            category = "Sport",
            variants = listOf("S", "M", "L", "XL"),
            imageUrl = "https://images.unsplash.com/photo-1506152983158-b4a74a01c721?w=500&auto=format&fit=crop&q=60",
            description = "Legging technique respirant haute performance. Ajoutez de fins détails réfléchissants ou votre marque de fitness sur le côté."
        ),
        Product(
            id = "coque",
            title = "Coque de smartphone personnalisée",
            minPrice = 15.0,
            maxPrice = 22.0,
            category = "Accessoires",
            variants = listOf("iPhone 13", "iPhone 14", "iPhone 15", "Samsung S23", "Samsung S24"),
            imageUrl = "https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=500&auto=format&fit=crop&q=60",
            description = "Coque ultra-résistante antichoc en silicone. Sublimez vos photos et vos patterns artistiques préférés avec une impression HD brillante."
        ),
        Product(
            id = "bougie",
            title = "Bougie aromatique personnalisée",
            minPrice = 20.0,
            maxPrice = 28.0,
            category = "Maison",
            variants = listOf("Vanilla Star", "Lavender Dusk", "Forest Drift"),
            imageUrl = "https://images.unsplash.com/photo-1603006905393-0130985227c9?w=500&auto=format&fit=crop&q=60",
            description = "Bougie coulée à la main en cire de soja naturelle. Personnalisez l'étiquette ainsi que la senteur boisée, florale ou épicée."
        ),
        Product(
            id = "tapis-yoga",
            title = "Tapis de yoga personnalisable",
            minPrice = 35.0,
            maxPrice = 45.0,
            category = "Sport",
            variants = listOf("Standard (4mm)", "Confort (6mm)"),
            imageUrl = "https://images.unsplash.com/photo-1592432678016-e910b452f9a2?w=500&auto=format&fit=crop&q=60",
            description = "Tapis biodégradable en TPE antidérapant. Les repères d'alignement ou vos graphismes spirituels gravés au laser durent à vie."
        ),
        Product(
            id = "bouteille",
            title = "Bouteille en inox réutilisable",
            minPrice = 20.0,
            maxPrice = 28.0,
            category = "Accessoires",
            variants = listOf("500ml", "750ml", "1L"),
            imageUrl = "https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=500&auto=format&fit=crop&q=60",
            description = "Bouteille isotherme double paroi inox. Garde au frais 24h et au chaud 12h. Ajoutez-y votre logo gravé ou imprimé."
        ),
        Product(
            id = "sac-besace",
            title = "Sac besace personnalisable",
            minPrice = 25.0,
            maxPrice = 32.0,
            category = "Accessoires",
            variants = listOf("Unique"),
            imageUrl = "https://images.unsplash.com/photo-1548036328-c9fa89d128fa?w=500&auto=format&fit=crop&q=60",
            description = "Sac messager robuste avec sangle ajustable et compartiments intelligents. Une surface idéale pour afficher vos créations."
        ),
        Product(
            id = "casquette",
            title = "Casquette personnalisable",
            minPrice = 18.0,
            maxPrice = 24.0,
            category = "Accessoires",
            variants = listOf("Ajustable"),
            imageUrl = "https://images.unsplash.com/photo-1588850561407-ed78c282e89b?w=500&auto=format&fit=crop&q=60",
            description = "Casquette trucker ou baseball classique, visière courbée. Idéal pour de superbes impressions de slogans ou d'illustrations brodées."
        ),
        Product(
            id = "taie-oreiller",
            title = "Taie d'oreiller personnalisée",
            minPrice = 16.0,
            maxPrice = 22.0,
            category = "Maison",
            variants = listOf("50x70cm", "65x65cm"),
            imageUrl = "https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?w=500&auto=format&fit=crop&q=60",
            description = "Taie d'oreiller en satin soyeux de bambou, protecteur pour les cheveux et la peau. Imprimez vos patterns favoris."
        ),
        Product(
            id = "poster",
            title = "Poster/affiche art mural",
            minPrice = 15.0,
            maxPrice = 25.0,
            category = "Maison",
            variants = listOf("A4", "A3", "A2"),
            imageUrl = "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=500&auto=format&fit=crop&q=60",
            description = "Papier d'art mat de fort grammage. Idéal pour des illustrations minimalistes, des citations motivantes ou vos dessins originaux de Cam's."
        ),
        Product(
            id = "pyjama",
            title = "Pyjama/sweat ensemble",
            minPrice = 40.0,
            maxPrice = 50.0,
            category = "Mode",
            variants = listOf("S", "M", "L", "XL"),
            imageUrl = "https://images.unsplash.com/photo-1562157873-818bc0726f68?w=500&auto=format&fit=crop&q=60",
            description = "Ensemble d'intérieur t-shirt et short ou pantalon ultra doux en viscose de canne à sucre pour des nuits légères."
        ),
        Product(
            id = "serviette",
            title = "Serviette de bain personnalisée",
            minPrice = 22.0,
            maxPrice = 30.0,
            category = "Maison",
            variants = listOf("70x140cm", "100x150cm"),
            imageUrl = "https://images.unsplash.com/photo-1563453392212-326f5e854473?w=500&auto=format&fit=crop&q=60",
            description = "Serviette éponge épaisse double-face bouclette jacquard. Parfaite pour la plage ou la salle de bain. Motifs d'une finesse incroyable."
        )
    )

    fun getAsJsonString(): String {
        return """
        [
          ${items.joinToString(",\n          ") { product ->
            """{
            "id": "${product.id}",
            "title": "${product.title}",
            "minPrice": ${product.minPrice},
            "maxPrice": ${product.maxPrice},
            "category": "${product.category}",
            "variants": [${product.variants.joinToString(", ") { "\"$it\"" }}]
          }"""
          }}
        ]
        """.trimIndent()
    }
}
