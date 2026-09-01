/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.stotra

import io.github.vedicmitra.core.common.model.ContentSource
import java.time.DayOfWeek

/**
 * A stotra (hymn / shloka) in the bundled library.
 *
 * @property id a stable identifier.
 * @property title the common name.
 * @property deity the deity or group it belongs to, used to group the library.
 * @property devanagari the verse(s) in Devanagari.
 * @property transliteration a roman transliteration, line-for-line with [devanagari].
 * @property significance a short note on what it is and when it's recited.
 * @property source the text this verse is taken from. No default: a new entry must decide, and
 *   [ContentSource.NotRecorded] is the honest answer where nobody has yet identified one.
 */
data class Stotra(
    val id: String,
    val title: String,
    val deity: String,
    val devanagari: String,
    val transliteration: String,
    val significance: String,
    val source: ContentSource,
)

/**
 * The bundled catalog of stotras — all traditional (public-domain) Sanskrit. Grouped by deity, with a
 * weekday-graha suggestion for "today's stotra". Only the verse text and a short original note are
 * bundled (no full modern translation), keeping the content free of third-party translation rights.
 *
 * `LargeClass` is suppressed here: this is a bundled data catalog whose length is verse content,
 * not logic complexity. Splitting the hymn list across objects/files would only scatter the data.
 */
@Suppress("LargeClass")
object StotraCatalog {
    /** Every stotra, clustered by deity in a sensible worship order. */
    val all: List<Stotra> =
        listOf(
            Stotra(
                source = ContentSource.NotRecorded,
                id = "ganesha_vakratunda",
                title = "Vakratunda Mahakaya",
                deity = "Ganesha",
                devanagari =
                    """
                    वक्रतुण्ड महाकाय
                    सूर्यकोटि समप्रभ ।
                    निर्विघ्नं कुरु मे देव
                    सर्वकार्येषु सर्वदा ॥
                    """.trimIndent(),
                transliteration =
                    """
                    vakratunda mahakaya
                    surya-koti samaprabha
                    nirvighnam kuru me deva
                    sarva-karyeshu sarvada
                    """.trimIndent(),
                significance = "A prayer to remove obstacles, invoked before beginning any task or worship.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "ganesha_shuklambara",
                title = "Shuklambaradharam",
                deity = "Ganesha",
                devanagari =
                    """
                    शुक्लाम्बरधरं विष्णुं
                    शशिवर्णं चतुर्भुजम् ।
                    प्रसन्नवदनं ध्यायेत्
                    सर्वविघ्नोपशान्तये ॥
                    """.trimIndent(),
                transliteration =
                    """
                    shuklambara-dharam vishnum
                    shashi-varnam chatur-bhujam
                    prasanna-vadanam dhyayet
                    sarva-vighnopashantaye
                    """.trimIndent(),
                significance = "A meditation on Ganesha for the calming of all obstacles, recited to open prayers.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "ganesha_gajananam",
                title = "Gajananam Bhutaganadi",
                deity = "Ganesha",
                devanagari =
                    """
                    गजाननं भूतगणादिसेवितं
                    कपित्थजम्बूफलचारुभक्षणम् ।
                    उमासुतं शोकविनाशकारणं
                    नमामि विघ्नेश्वरपादपङ्कजम् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    gajananam bhuta-ganadi-sevitam
                    kapittha-jambu-phala-charu-bhakshanam
                    uma-sutam shoka-vinasha-karanam
                    namami vighneshvara-pada-pankajam
                    """.trimIndent(),
                significance = "The dhyana verse to elephant-faced Ganesha; recited to begin any worship.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "mahamrityunjaya",
                title = "Mahamrityunjaya Mantra",
                deity = "Shiva",
                devanagari =
                    """
                    ॐ त्र्यम्बकं यजामहे
                    सुगन्धिं पुष्टिवर्धनम् ।
                    उर्वारुकमिव बन्धनान्
                    मृत्योर्मुक्षीय माऽमृतात् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    om tryambakam yajamahe
                    sugandhim pushti-vardhanam
                    urvarukamiva bandhanan
                    mrityor mukshiya maamritat
                    """.trimIndent(),
                significance =
                    "The great victory-over-death mantra to three-eyed Shiva, prayed for healing and longevity.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "shiva_karpura",
                title = "Karpura Gauram",
                deity = "Shiva",
                devanagari =
                    """
                    कर्पूरगौरं करुणावतारं
                    संसारसारं भुजगेन्द्रहारम् ।
                    सदा वसन्तं हृदयारविन्दे
                    भवं भवानीसहितं नमामि ॥
                    """.trimIndent(),
                transliteration =
                    """
                    karpura-gauram karunavataram
                    samsara-saram bhujagendra-haram
                    sada vasantam hridayaravinde
                    bhavam bhavani-sahitam namami
                    """.trimIndent(),
                significance = "A camphor-white image of Shiva with Parvati, dwelling ever in the lotus of the heart.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "shiva_panchakshara",
                title = "Nagendraharaya",
                deity = "Shiva",
                devanagari =
                    """
                    नागेन्द्रहाराय त्रिलोचनाय
                    भस्माङ्गरागाय महेश्वराय ।
                    नित्याय शुद्धाय दिगम्बराय
                    तस्मै न काराय नमः शिवाय ॥
                    """.trimIndent(),
                transliteration =
                    """
                    nagendra-haraya tri-lochanaya
                    bhasmanga-ragaya maheshvaraya
                    nityaya shuddhaya digambaraya
                    tasmai na-karaya namah shivaya
                    """.trimIndent(),
                significance = "The opening verse of the Shiva Panchakshara Stotra, glorifying \"Namah Shivaya\".",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "shiva_lingashtakam",
                title = "Lingashtakam",
                deity = "Shiva",
                devanagari =
                    """
                    ब्रह्ममुरारिसुरार्चितलिङ्गं
                    निर्मलभासितशोभितलिङ्गम् ।
                    जन्मजदुःखविनाशकलिङ्गं
                    तत्प्रणमामि सदाशिवलिङ्गम् ॥
                    देवमुनिप्रवरार्चितलिङ्गं
                    कामदहनकरुणाकरलिङ्गम् ।
                    रावणदर्पविनाशनलिङ्गं
                    तत्प्रणमामि सदाशिवलिङ्गम् ॥
                    सर्वसुगन्धसुलेपितलिङ्गं
                    बुद्धिविवर्धनकारणलिङ्गम् ।
                    सिद्धसुरासुरवन्दितलिङ्गं
                    तत्प्रणमामि सदाशिवलिङ्गम् ॥
                    कनकमहामणिभूषितलिङ्गं
                    फणिपतिवेष्टितशोभितलिङ्गम् ।
                    दक्षसुयज्ञविनाशनलिङ्गं
                    तत्प्रणमामि सदाशिवलिङ्गम् ॥
                    कुङ्कुमचन्दनलेपितलिङ्गं
                    पङ्कजहारसुशोभितलिङ्गम् ।
                    सञ्चितपापविनाशनलिङ्गं
                    तत्प्रणमामि सदाशिवलिङ्गम् ॥
                    देवगणार्चितसेवितलिङ्गं
                    भावैर्भक्तिभिरेव च लिङ्गम् ।
                    दिनकरकोटिप्रभाकरलिङ्गं
                    तत्प्रणमामि सदाशिवलिङ्गम् ॥
                    अष्टदलोपरिवेष्टितलिङ्गं
                    सर्वसमुद्भवकारणलिङ्गम् ।
                    अष्टदरिद्रविनाशनलिङ्गं
                    तत्प्रणमामि सदाशिवलिङ्गम् ॥
                    सुरगुरुसुरवरपूजितलिङ्गं
                    सुरवनपुष्पसदार्चितलिङ्गम् ।
                    परात्परं परमात्मकलिङ्गं
                    तत्प्रणमामि सदाशिवलिङ्गम् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    brahma-murari-surarchita-lingam
                    nirmala-bhasita-shobhita-lingam
                    janmaja-duhkha-vinashaka-lingam
                    tat pranamami sadashiva-lingam
                    deva-muni-pravararchita-lingam
                    kama-dahana-karunakara-lingam
                    ravana-darpa-vinashana-lingam
                    tat pranamami sadashiva-lingam
                    sarva-sugandha-sulepita-lingam
                    buddhi-vivardhana-karana-lingam
                    siddha-surasura-vandita-lingam
                    tat pranamami sadashiva-lingam
                    kanaka-mahamani-bhushita-lingam
                    phanipati-veshtita-shobhita-lingam
                    daksha-suyajna-vinashana-lingam
                    tat pranamami sadashiva-lingam
                    kunkuma-chandana-lepita-lingam
                    pankaja-hara-sushobhita-lingam
                    sanchita-papa-vinashana-lingam
                    tat pranamami sadashiva-lingam
                    deva-ganarchita-sevita-lingam
                    bhavair-bhaktibhir-eva cha lingam
                    dinakara-koti-prabhakara-lingam
                    tat pranamami sadashiva-lingam
                    ashtadalopari-veshtita-lingam
                    sarva-samudbhava-karana-lingam
                    ashta-daridra-vinashana-lingam
                    tat pranamami sadashiva-lingam
                    sura-guru-suravara-pujita-lingam
                    suravana-pushpa-sadarchita-lingam
                    paratparam paramatmaka-lingam
                    tat pranamami sadashiva-lingam
                    """.trimIndent(),
                significance = "Eight verses to the Shiva-linga as the formless Absolute; sung on Shivaratri.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "shiva_nirvana_shatkam",
                title = "Nirvana Shatkam",
                deity = "Shiva",
                devanagari =
                    """
                    मनोबुद्ध्यहङ्कारचित्तानि नाहं
                    न च श्रोत्रजिह्वे न च घ्राणनेत्रे ।
                    न च व्योम भूमिर्न तेजो न वायुः
                    चिदानन्दरूपः शिवोऽहं शिवोऽहम् ॥
                    न च प्राणसंज्ञो न वै पञ्चवायुः
                    न वा सप्तधातुर्न वा पञ्चकोशः ।
                    न वाक्पाणिपादौ न चोपस्थपायू
                    चिदानन्दरूपः शिवोऽहं शिवोऽहम् ॥
                    न मे द्वेषरागौ न मे लोभमोहौ
                    मदो नैव मे नैव मात्सर्यभावः ।
                    न धर्मो न चार्थो न कामो न मोक्षः
                    चिदानन्दरूपः शिवोऽहं शिवोऽहम् ॥
                    न पुण्यं न पापं न सौख्यं न दुःखं
                    न मन्त्रो न तीर्थं न वेदा न यज्ञाः ।
                    अहं भोजनं नैव भोज्यं न भोक्ता
                    चिदानन्दरूपः शिवोऽहं शिवोऽहम् ॥
                    न मृत्युर्न शङ्का न मे जातिभेदः
                    पिता नैव मे नैव माता न जन्म ।
                    न बन्धुर्न मित्रं गुरुर्नैव शिष्यः
                    चिदानन्दरूपः शिवोऽहं शिवोऽहम् ॥
                    अहं निर्विकल्पो निराकाररूपो
                    विभुत्वाच्च सर्वत्र सर्वेन्द्रियाणाम् ।
                    न चासङ्गतं नैव मुक्तिर्न मेयः
                    चिदानन्दरूपः शिवोऽहं शिवोऽहम् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    mano-buddhy-ahankara-chittani naham
                    na cha shrotra-jihve na cha ghrana-netre
                    na cha vyoma bhumir na tejo na vayuh
                    chidananda-rupah shivo'ham shivo'ham
                    na cha prana-sanjno na vai pancha-vayuh
                    na va sapta-dhatur na va pancha-koshah
                    na vak-pani-padau na chopastha-payu
                    chidananda-rupah shivo'ham shivo'ham
                    na me dvesha-ragau na me lobha-mohau
                    mado naiva me naiva matsarya-bhavah
                    na dharmo na chartho na kamo na mokshah
                    chidananda-rupah shivo'ham shivo'ham
                    na punyam na papam na saukhyam na duhkham
                    na mantro na tirtham na veda na yajnah
                    aham bhojanam naiva bhojyam na bhokta
                    chidananda-rupah shivo'ham shivo'ham
                    na mrityur na shanka na me jati-bhedah
                    pita naiva me naiva mata na janma
                    na bandhur na mitram gurur naiva shishyah
                    chidananda-rupah shivo'ham shivo'ham
                    aham nirvikalpo nirakara-rupo
                    vibhutvach cha sarvatra sarvendriyanam
                    na chasangatam naiva muktir na meyah
                    chidananda-rupah shivo'ham shivo'ham
                    """.trimIndent(),
                significance = "Adi Shankara's Advaita hymn on the true Self: I am consciousness and bliss.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "vishnu_shantakaram",
                title = "Shantakaram",
                deity = "Vishnu",
                devanagari =
                    """
                    शान्ताकारं भुजगशयनं
                    पद्मनाभं सुरेशम् ।
                    विश्वाधारं गगनसदृशं
                    मेघवर्णं शुभाङ्गम् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    shantakaram bhujaga-shayanam
                    padmanabham suresham
                    vishvadharam gagana-sadrisham
                    megha-varnam shubhangam
                    """.trimIndent(),
                significance = "A serene meditation on Vishnu reclining on the cosmic serpent Shesha.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "krishna_vasudeva",
                title = "Vasudevasutam",
                deity = "Krishna",
                devanagari =
                    """
                    वसुदेवसुतं देवं
                    कंसचाणूरमर्दनम् ।
                    देवकीपरमानन्दं
                    कृष्णं वन्दे जगद्गुरुम् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    vasudeva-sutam devam
                    kamsa-chanura-mardanam
                    devaki-paramanandam
                    krishnam vande jagad-gurum
                    """.trimIndent(),
                significance = "A vandana to Krishna, teacher of the world and joy of Devaki.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "krishna_madhurashtakam",
                title = "Madhurashtakam",
                deity = "Krishna",
                devanagari =
                    """
                    अधरं मधुरं वदनं मधुरं
                    नयनं मधुरं हसितं मधुरम् ।
                    हृदयं मधुरं गमनं मधुरं
                    मधुराधिपतेरखिलं मधुरम् ॥
                    वचनं मधुरं चरितं मधुरं
                    वसनं मधुरं वलितं मधुरम् ।
                    चलितं मधुरं भ्रमितं मधुरं
                    मधुराधिपतेरखिलं मधुरम् ॥
                    वेणुर्मधुरो रेणुर्मधुरः
                    पाणिर्मधुरः पादौ मधुरौ ।
                    नृत्यं मधुरं सख्यं मधुरं
                    मधुराधिपतेरखिलं मधुरम् ॥
                    गीतं मधुरं पीतं मधुरं
                    भुक्तं मधुरं सुप्तं मधुरम् ।
                    रूपं मधुरं तिलकं मधुरं
                    मधुराधिपतेरखिलं मधुरम् ॥
                    करणं मधुरं तरणं मधुरं
                    हरणं मधुरं रमणं मधुरम् ।
                    वमितं मधुरं शमितं मधुरं
                    मधुराधिपतेरखिलं मधुरम् ॥
                    गुञ्जा मधुरा माला मधुरा
                    यमुना मधुरा वीची मधुरा ।
                    सलिलं मधुरं कमलं मधुरं
                    मधुराधिपतेरखिलं मधुरम् ॥
                    गोपी मधुरा लीला मधुरा
                    युक्तं मधुरं मुक्तं मधुरम् ।
                    दृष्टं मधुरं शिष्टं मधुरं
                    मधुराधिपतेरखिलं मधुरम् ॥
                    गोपा मधुरा गावो मधुरा
                    यष्टिर्मधुरा सृष्टिर्मधुरा ।
                    दलितं मधुरं फलितं मधुरं
                    मधुराधिपतेरखिलं मधुरम् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    adharam madhuram vadanam madhuram
                    nayanam madhuram hasitam madhuram
                    hridayam madhuram gamanam madhuram
                    madhuradhipater akhilam madhuram
                    vachanam madhuram charitam madhuram
                    vasanam madhuram valitam madhuram
                    chalitam madhuram bhramitam madhuram
                    madhuradhipater akhilam madhuram
                    venur madhuro renur madhurah
                    panir madhurah padau madhurau
                    nrityam madhuram sakhyam madhuram
                    madhuradhipater akhilam madhuram
                    gitam madhuram pitam madhuram
                    bhuktam madhuram suptam madhuram
                    rupam madhuram tilakam madhuram
                    madhuradhipater akhilam madhuram
                    karanam madhuram taranam madhuram
                    haranam madhuram ramanam madhuram
                    vamitam madhuram shamitam madhuram
                    madhuradhipater akhilam madhuram
                    gunja madhura mala madhura
                    yamuna madhura vichi madhura
                    salilam madhuram kamalam madhuram
                    madhuradhipater akhilam madhuram
                    gopi madhura lila madhura
                    yuktam madhuram muktam madhuram
                    drishtam madhuram shishtam madhuram
                    madhuradhipater akhilam madhuram
                    gopa madhura gavo madhura
                    yashtir madhura srishtir madhura
                    dalitam madhuram phalitam madhuram
                    madhuradhipater akhilam madhuram
                    """.trimIndent(),
                significance = "Adi Shankara's eight verses on the sweetness of Krishna, Lord of Mathura.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "rama_nama",
                title = "Shri Rama Rama Rameti",
                deity = "Rama",
                devanagari =
                    """
                    श्रीरामं रामरामेति
                    रमे रामे मनोरमे ।
                    सहस्रनाम तत्तुल्यं
                    रामनाम वरानने ॥
                    """.trimIndent(),
                transliteration =
                    """
                    shri-ramam rama-rameti
                    rame rame manorame
                    sahasra-nama tat-tulyam
                    rama-nama varanane
                    """.trimIndent(),
                significance = "The verse in which repeating \"Rama\" is said to equal a thousand names of the Lord.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "devi_ya_devi",
                title = "Ya Devi Sarvabhuteshu",
                deity = "Devi",
                devanagari =
                    """
                    या देवी सर्वभूतेषु
                    शक्तिरूपेण संस्थिता ।
                    नमस्तस्यै नमस्तस्यै
                    नमस्तस्यै नमो नमः ॥
                    """.trimIndent(),
                transliteration =
                    """
                    ya devi sarva-bhuteshu
                    shakti-rupena samsthita
                    namas-tasyai namas-tasyai
                    namas-tasyai namo namah
                    """.trimIndent(),
                significance = "From the Devi Mahatmyam, saluting the Goddess who dwells in all beings as power.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "durga_sarva_mangala",
                title = "Sarva Mangala Mangalye",
                deity = "Devi",
                devanagari =
                    """
                    सर्वमङ्गलमाङ्गल्ये
                    शिवे सर्वार्थसाधिके ।
                    शरण्ये त्र्यम्बके गौरि
                    नारायणि नमोऽस्तु ते ॥
                    """.trimIndent(),
                transliteration =
                    """
                    sarva-mangala-mangalye
                    shive sarvartha-sadhike
                    sharanye tryambake gauri
                    narayani namo'stu te
                    """.trimIndent(),
                significance = "A beloved prayer to the auspicious Goddess who fulfils all aims and grants refuge.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "lakshmi",
                title = "Namaste'stu Mahamaye",
                deity = "Lakshmi",
                devanagari =
                    """
                    नमस्तेऽस्तु महामाये
                    श्रीपीठे सुरपूजिते ।
                    शङ्खचक्रगदाहस्ते
                    महालक्ष्मि नमोऽस्तु ते ॥
                    """.trimIndent(),
                transliteration =
                    """
                    namaste'stu maha-maye
                    shri-pithe sura-pujite
                    shankha-chakra-gada-haste
                    maha-lakshmi namo'stu te
                    """.trimIndent(),
                significance = "A salutation to Mahalakshmi, goddess of abundance; recited on Fridays and Diwali.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "saraswati",
                title = "Ya Kundendu Tushara",
                deity = "Saraswati",
                devanagari =
                    """
                    या कुन्देन्दुतुषारहारधवला
                    या शुभ्रवस्त्रावृता ।
                    या वीणावरदण्डमण्डितकरा
                    या श्वेतपद्मासना ॥
                    """.trimIndent(),
                transliteration =
                    """
                    ya kundendu-tushara-hara-dhavala
                    ya shubhra-vastravrita
                    ya vina-vara-danda-mandita-kara
                    ya shveta-padmasana
                    """.trimIndent(),
                significance = "A vandana to Saraswati, goddess of knowledge, music and speech; recited by students.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "gayatri",
                title = "Gayatri Mantra",
                deity = "Surya",
                devanagari =
                    """
                    ॐ भूर्भुवः स्वः ।
                    तत्सवितुर्वरेण्यं
                    भर्गो देवस्य धीमहि ।
                    धियो यो नः प्रचोदयात् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    om bhur bhuvah svah
                    tat savitur varenyam
                    bhargo devasya dhimahi
                    dhiyo yo nah prachodayat
                    """.trimIndent(),
                significance = "The supreme Vedic prayer to Savitr (the Sun) to awaken and guide the mind.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "guru",
                title = "Gurur Brahma",
                deity = "Guru",
                devanagari =
                    """
                    गुरुर्ब्रह्मा गुरुर्विष्णुः
                    गुरुर्देवो महेश्वरः ।
                    गुरुः साक्षात् परब्रह्म
                    तस्मै श्रीगुरवे नमः ॥
                    """.trimIndent(),
                transliteration =
                    """
                    gurur brahma gurur vishnuh
                    gurur devo maheshvarah
                    guruh sakshat para-brahma
                    tasmai shri-gurave namah
                    """.trimIndent(),
                significance = "Honours the guru as Brahma, Vishnu and Shiva; recited on Guru Purnima and Thursdays.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "hanuman",
                title = "Manojavam Marutatulya",
                deity = "Hanuman",
                devanagari =
                    """
                    मनोजवं मारुततुल्यवेगं
                    जितेन्द्रियं बुद्धिमतां वरिष्ठम् ।
                    वातात्मजं वानरयूथमुख्यं
                    श्रीरामदूतं शरणं प्रपद्ये ॥
                    """.trimIndent(),
                transliteration =
                    """
                    manojavam maruta-tulya-vegam
                    jitendriyam buddhimatam varishtham
                    vatatmajam vanara-yutha-mukhyam
                    shri-rama-dutam sharanam prapadye
                    """.trimIndent(),
                significance = "A dhyana on Hanuman — swift, wise, and devoted; recited on Tuesdays and Saturdays.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "hanuman_chalisa",
                title = "Hanuman Chalisa",
                deity = "Hanuman",
                devanagari =
                    """
                    श्रीगुरु चरन सरोज रज, निज मनु मुकुरु सुधारि ।
                    बरनउँ रघुबर बिमल जसु, जो दायकु फल चारि ॥
                    बुद्धिहीन तनु जानिके, सुमिरौं पवन-कुमार ।
                    बल बुधि बिद्या देहु मोहिं, हरहु कलेस बिकार ॥
                    जय हनुमान ज्ञान गुन सागर, जय कपीस तिहुँ लोक उजागर ॥
                    राम दूत अतुलित बल धामा, अंजनि-पुत्र पवनसुत नामा ॥
                    महाबीर बिक्रम बजरंगी, कुमति निवार सुमति के संगी ॥
                    कंचन बरन बिराज सुबेसा, कानन कुंडल कुंचित केसा ॥
                    हाथ बज्र औ ध्वजा बिराजै, काँधे मूँज जनेऊ साजै ॥
                    संकर सुवन केसरीनंदन, तेज प्रताप महा जग बंदन ॥
                    बिद्यावान गुनी अति चातुर, राम काज करिबे को आतुर ॥
                    प्रभु चरित्र सुनिबे को रसिया, राम लखन सीता मन बसिया ॥
                    सूक्ष्म रूप धरि सियहिं दिखावा, बिकट रूप धरि लंक जरावा ॥
                    भीम रूप धरि असुर सँहारे, रामचंद्र के काज सँवारे ॥
                    लाय सजीवन लखन जियाये, श्रीरघुबीर हरषि उर लाये ॥
                    रघुपति कीन्ही बहुत बड़ाई, तुम मम प्रिय भरतहि सम भाई ॥
                    सहस बदन तुम्हरो जस गावैं, अस कहि श्रीपति कंठ लगावैं ॥
                    सनकादिक ब्रह्मादि मुनीसा, नारद सारद सहित अहीसा ॥
                    जम कुबेर दिगपाल जहाँ ते, कबि कोबिद कहि सके कहाँ ते ॥
                    तुम उपकार सुग्रीवहिं कीन्हा, राम मिलाय राज पद दीन्हा ॥
                    तुम्हरो मंत्र बिभीषन माना, लंकेश्वर भए सब जग जाना ॥
                    जुग सहस्र जोजन पर भानू, लील्यो ताहि मधुर फल जानू ॥
                    प्रभु मुद्रिका मेलि मुख माहीं, जलधि लाँघि गये अचरज नाहीं ॥
                    दुर्गम काज जगत के जेते, सुगम अनुग्रह तुम्हरे तेते ॥
                    राम दुआरे तुम रखवारे, होत न आज्ञा बिनु पैसारे ॥
                    सब सुख लहै तुम्हारी सरना, तुम रच्छक काहू को डर ना ॥
                    आपन तेज सम्हारो आपै, तीनों लोक हाँक तें काँपै ॥
                    भूत पिसाच निकट नहिं आवै, महाबीर जब नाम सुनावै ॥
                    नासै रोग हरै सब पीरा, जपत निरंतर हनुमत बीरा ॥
                    संकट तें हनुमान छुड़ावै, मन क्रम बचन ध्यान जो लावै ॥
                    सब पर राम तपस्वी राजा, तिन के काज सकल तुम साजा ॥
                    और मनोरथ जो कोई लावै, सोइ अमित जीवन फल पावै ॥
                    चारों जुग परताप तुम्हारा, है परसिद्ध जगत उजियारा ॥
                    साधु संत के तुम रखवारे, असुर निकंदन राम दुलारे ॥
                    अष्ट सिद्धि नौ निधि के दाता, अस बर दीन्ह जानकी माता ॥
                    राम रसायन तुम्हरे पासा, सदा रहो रघुपति के दासा ॥
                    तुम्हरे भजन राम को पावै, जनम जनम के दुख बिसरावै ॥
                    अंत काल रघुबर पुर जाई, जहाँ जन्म हरिभक्त कहाई ॥
                    और देवता चित्त न धरई, हनुमत सेइ सर्ब सुख करई ॥
                    संकट कटै मिटै सब पीरा, जो सुमिरै हनुमत बलबीरा ॥
                    जै जै जै हनुमान गोसाईं, कृपा करहु गुरुदेव की नाईं ॥
                    जो सत बार पाठ कर कोई, छूटहि बंदि महा सुख होई ॥
                    जो यह पढ़ै हनुमान चालीसा, होय सिद्धि साखी गौरीसा ॥
                    तुलसीदास सदा हरि चेरा, कीजै नाथ हृदय महँ डेरा ॥
                    पवनतनय संकट हरन, मंगल मूरति रूप ।
                    राम लखन सीता सहित, हृदय बसहु सुर भूप ॥
                    """.trimIndent(),
                transliteration =
                    """
                    shri guru charan saroj raj, nija manu mukuru sudhari
                    baranau raghubar bimal jasu, jo dayaku phal chari
                    buddhihin tanu janike, sumirau pavan-kumar
                    bal budhi bidya dehu mohi, harahu kales bikar
                    jai hanuman gyan gun sagar, jai kapis tihu lok ujagar
                    ram dut atulit bal dhama, anjani-putra pavanasut nama
                    mahabir bikram bajrangi, kumati nivar sumati ke sangi
                    kanchan baran biraj subesa, kanan kundal kunchit kesa
                    hath bajra au dhvaja birajai, kandhe munj janeu sajai
                    sankar suvan kesari-nandan, tej pratap maha jag bandan
                    bidyavan guni ati chatur, ram kaj karibe ko atur
                    prabhu charitra sunibe ko rasiya, ram lakhan sita man basiya
                    sukshma rup dhari siyahi dikhava, bikat rup dhari lank jarava
                    bhim rup dhari asur sanhare, ramchandra ke kaj sanvare
                    lay sajivan lakhan jiyaye, shri raghubir harashi ur laye
                    raghupati kinhi bahut badai, tum mam priya bharatahi sam bhai
                    sahas badan tumharo jas gavai, as kahi shripati kanth lagavai
                    sanakadik brahmadi munisa, narad sarad sahit ahisa
                    jam kuber digpal jaha te, kabi kobid kahi sake kaha te
                    tum upkar sugrivahi kinha, ram milay raj pad dinha
                    tumharo mantra bibhishan mana, lankeshvar bhae sab jag jana
                    jug sahasra jojan par bhanu, lilyo tahi madhur phal janu
                    prabhu mudrika meli mukh mahi, jaladhi langhi gaye achraj nahi
                    durgam kaj jagat ke jete, sugam anugrah tumhre tete
                    ram duare tum rakhvare, hot na agya binu paisare
                    sab sukh lahai tumhari sarna, tum rachchhak kahu ko dar na
                    apan tej samharo apai, tinon lok hank te kanpai
                    bhut pisach nikat nahi avai, mahabir jab nam sunavai
                    nasai rog harai sab pira, japat nirantar hanumat bira
                    sankat te hanuman chhudavai, man kram bachan dhyan jo lavai
                    sab par ram tapasvi raja, tin ke kaj sakal tum saja
                    aur manorath jo koi lavai, soi amit jivan phal pavai
                    charon jug partap tumhara, hai parsiddh jagat ujiyara
                    sadhu sant ke tum rakhvare, asur nikandan ram dulare
                    asht siddhi nau nidhi ke data, as bar dinh janaki mata
                    ram rasayan tumhre pasa, sada raho raghupati ke dasa
                    tumhre bhajan ram ko pavai, janam janam ke dukh bisravai
                    ant kal raghubar pur jai, jaha janm haribhakt kahai
                    aur devta chitt na dharai, hanumat sei sarb sukh karai
                    sankat katai mitai sab pira, jo sumirai hanumat balbira
                    jai jai jai hanuman gosai, kripa karahu gurudev ki nai
                    jo sat bar path kar koi, chhutahi bandi maha sukh hoi
                    jo yah padhai hanuman chalisa, hoy siddhi sakhi gaurisa
                    tulsidas sada hari chera, kijai nath hriday mahan dera
                    pavan-tanay sankat haran, mangal murati rup
                    ram lakhan sita sahit, hriday basahu sur bhup
                    """.trimIndent(),
                significance = "Tulsidas's forty-verse hymn to Hanuman; chanted on Tuesdays and Saturdays.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "navagraha",
                title = "Navagraha Stotra",
                deity = "Navagraha",
                devanagari =
                    """
                    जपाकुसुमसंकाशं काश्यपेयं महाद्युतिम् ।
                    तमोऽरिं सर्वपापघ्नं प्रणतोऽस्मि दिवाकरम् ॥
                    दधिशङ्खतुषाराभं क्षीरोदार्णवसम्भवम् ।
                    नमामि शशिनं सोमं शम्भोर्मुकुटभूषणम् ॥
                    धरणीगर्भसम्भूतं विद्युत्कान्तिसमप्रभम् ।
                    कुमारं शक्तिहस्तं तं मङ्गलं प्रणमाम्यहम् ॥
                    प्रियङ्गुकलिकाश्यामं रूपेणाप्रतिमं बुधम् ।
                    सौम्यं सौम्यगुणोपेतं तं बुधं प्रणमाम्यहम् ॥
                    देवानां च ऋषीणां च गुरुं काञ्चनसन्निभम् ।
                    बुद्धिभूतं त्रिलोकेशं तं नमामि बृहस्पतिम् ॥
                    हिमकुन्दमृणालाभं दैत्यानां परमं गुरुम् ।
                    सर्वशास्त्रप्रवक्तारं भार्गवं प्रणमाम्यहम् ॥
                    नीलाञ्जनसमाभासं रविपुत्रं यमाग्रजम् ।
                    छायामार्तण्डसम्भूतं तं नमामि शनैश्चरम् ॥
                    अर्धकायं महावीर्यं चन्द्रादित्यविमर्दनम् ।
                    सिंहिकागर्भसम्भूतं तं राहुं प्रणमाम्यहम् ॥
                    पलाशपुष्पसंकाशं तारकाग्रहमस्तकम् ।
                    रौद्रं रौद्रात्मकं घोरं तं केतुं प्रणमाम्यहम् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    japa-kusuma-sankasham kashyapeyam maha-dyutim
                    tamo'rim sarva-papaghnam pranato'smi divakaram
                    dadhi-shankha-tusharabham kshirodarnava-sambhavam
                    namami shashinam somam shambhor-mukuta-bhushanam
                    dharani-garbha-sambhutam vidyut-kanti-samaprabham
                    kumaram shakti-hastam tam mangalam pranamamy-aham
                    priyangu-kalika-shyamam rupena-apratimam budham
                    saumyam saumya-gunopetam tam budham pranamamy-aham
                    devanam cha rishinam cha gurum kanchana-sannibham
                    buddhi-bhutam tri-lokesham tam namami brihaspatim
                    hima-kunda-mrinalabham daityanam paramam gurum
                    sarva-shastra-pravaktaram bhargavam pranamamy-aham
                    nilanjana-samabhasam ravi-putram yamagrajam
                    chhaya-martanda-sambhutam tam namami shanaischaram
                    ardha-kayam maha-viryam chandraditya-vimardanam
                    simhika-garbha-sambhutam tam rahum pranamamy-aham
                    palasha-pushpa-sankasham tarakagraha-mastakam
                    raudram raudratmakam ghoram tam ketum pranamamy-aham
                    """.trimIndent(),
                significance = "The nine short verses to the nine grahas (Sun through Ketu), one couplet for each.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "shanti_sarve",
                title = "Sarve Bhavantu Sukhinah",
                deity = "Shanti & Universal",
                devanagari =
                    """
                    ॐ सर्वे भवन्तु सुखिनः
                    सर्वे सन्तु निरामयाः ।
                    सर्वे भद्राणि पश्यन्तु
                    मा कश्चिद्दुःखभाग्भवेत् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    om sarve bhavantu sukhinah
                    sarve santu niramayah
                    sarve bhadrani pashyantu
                    ma kashchid duhkha-bhag bhavet
                    """.trimIndent(),
                significance = "A universal prayer that all beings be happy, healthy, and free from suffering.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "shanti_asato",
                title = "Asato Ma Sadgamaya",
                deity = "Shanti & Universal",
                devanagari =
                    """
                    ॐ असतो मा सद्गमय ।
                    तमसो मा ज्योतिर्गमय ।
                    मृत्योर्मा अमृतं गमय ॥
                    """.trimIndent(),
                transliteration =
                    """
                    om asato ma sad-gamaya
                    tamaso ma jyotir-gamaya
                    mrityor ma amritam gamaya
                    """.trimIndent(),
                significance =
                    "From the Brihadaranyaka Upanishad — lead me from the unreal to the real, dark to light.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "shanti_purnam",
                title = "Purnamadah Purnamidam",
                deity = "Shanti & Universal",
                devanagari =
                    """
                    ॐ पूर्णमदः पूर्णमिदं
                    पूर्णात्पूर्णमुदच्यते ।
                    पूर्णस्य पूर्णमादाय
                    पूर्णमेवावशिष्यते ॥
                    """.trimIndent(),
                transliteration =
                    """
                    om purnam-adah purnam-idam
                    purnat purnam-udachyate
                    purnasya purnam-adaya
                    purnam-eva-avashishyate
                    """.trimIndent(),
                significance = "The Isha Upanishad invocation on the fullness of the Absolute.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "twameva",
                title = "Tvameva Mata",
                deity = "Shanti & Universal",
                devanagari =
                    """
                    त्वमेव माता च पिता त्वमेव
                    त्वमेव बन्धुश्च सखा त्वमेव ।
                    त्वमेव विद्या द्रविणं त्वमेव
                    त्वमेव सर्वं मम देवदेव ॥
                    """.trimIndent(),
                transliteration =
                    """
                    tvameva mata cha pita tvameva
                    tvameva bandhush cha sakha tvameva
                    tvameva vidya dravinam tvameva
                    tvameva sarvam mama deva-deva
                    """.trimIndent(),
                significance = "A surrender prayer — you alone are mother, father, friend, knowledge, my all.",
            ),
            Stotra(
                source = ContentSource.NotRecorded,
                id = "brahmarpanam",
                title = "Brahmarpanam",
                deity = "Shanti & Universal",
                devanagari =
                    """
                    ब्रह्मार्पणं ब्रह्म हविः
                    ब्रह्माग्नौ ब्रह्मणा हुतम् ।
                    ब्रह्मैव तेन गन्तव्यं
                    ब्रह्मकर्मसमाधिना ॥
                    """.trimIndent(),
                transliteration =
                    """
                    brahmarpanam brahma havih
                    brahmagnau brahmana hutam
                    brahmaiva tena gantavyam
                    brahma-karma-samadhina
                    """.trimIndent(),
                significance = "A Bhagavad Gita verse (4.24) recited before meals — all is Brahman.",
            ),
        )

    private val byId: Map<String, Stotra> = all.associateBy { it.id }

    /** The library grouped by deity, preserving the catalogue's worship order. */
    val byDeity: Map<String, List<Stotra>> = all.groupBy { it.deity }

    /** The stotra with [id], or `null` if it isn't in the catalog. */
    fun byId(id: String): Stotra? = byId[id]

    /**
     * The "today's stotra" for [day], chosen by the weekday's ruling graha/deity: Sunday–Surya,
     * Monday–Shiva/Chandra, Tuesday–Hanuman/Mangala, Wednesday–Vishnu/Budha, Thursday–Guru,
     * Friday–Lakshmi/Shukra, Saturday–the Navagraha (Shani).
     */
    fun forWeekday(day: DayOfWeek): Stotra {
        val id =
            when (day) {
                DayOfWeek.SUNDAY -> "gayatri"
                DayOfWeek.MONDAY -> "shiva_karpura"
                DayOfWeek.TUESDAY -> "hanuman_chalisa"
                DayOfWeek.WEDNESDAY -> "vishnu_shantakaram"
                DayOfWeek.THURSDAY -> "guru"
                DayOfWeek.FRIDAY -> "lakshmi"
                DayOfWeek.SATURDAY -> "navagraha"
            }
        return requireNotNull(byId[id]) { "weekday stotra $id missing from catalog" }
    }
}
