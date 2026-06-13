package com.aegis.ielts.features.listening.data

import com.aegis.ielts.features.listening.data.*

object MockExamData {

    data class MockTest(
        val script: String,
        val section: ListeningSection
    )

    val tests = listOf(
        // Script 1: Campus Registration
        MockTest(
            script = """
                Welcome to the Aegis campus. I'm the student receptionist. Let's get your registration sorted. 
                First, I have your family name down on the initial form as Hemmington, but I see a correction here. Let me update that to your official family name: HEMINGWAY. That's H-E-M-I-N-G-W-A-Y. 
                And for your contact number, we previously had 07700900088 on file. However, you recently requested a change, so we will use 07700900077 moving forward.
                Now, regarding the nature reserve trip next week, be aware there are major traffic delays expected. 
                We usually worry about seasonal flooding or wildlife crossings this time of year, but the primary cause right now is actually bridge construction on the main road.
                Once you arrive, the reserve café is quite popular. We originally planned to have it open throughout the year, but due to staffing it's open to the public on weekends only.
                Let me help you with the campus map. The Student Help Center Office was formerly located at building A, but it has now been permanently moved to building B, right next to the east gate.
                And the Main Lecture Hall Complex is building C, right in the center of the campus. 
                For your history of architecture elective, you'll study different designs. For example, the Gothic Arches System falls squarely under the MEDIEVAL period. In contrast, the Steel Beam Foundations are distinctly MODERN.
                Oh, and a quick update on the library. The new opening hour on Sundays used to be 9:00 AM, but starting this term, it is exactly 10:00 AM. 
                If you need to return books after hours, the Book Return Box location is right at the ENTRANCE.
                That covers everything. Good luck with your studies!
            """.trimIndent(),
            section = ListeningSection(
                sectionNumber = 1,
                environment = ListeningEnvironment.SOCIAL_DIALOGUE,
                accent = Accent.STANDARD,
                audioAssetPath = "audio/section_1.mp3",
                questions = listOf(
                    ListeningQuestion.FormCompletion(id = "Q1_FORM_NAME", instruction = "Write ONE WORD ONLY.", questionText = "Family Name", correctAnswer = "HEMINGWAY"),
                    ListeningQuestion.FormCompletion(id = "Q1_FORM_PHONE", instruction = "Write NUMBERS ONLY.", questionText = "Contact Number", correctAnswer = "07700900077", charLimit = 11),
                    ListeningQuestion.MultipleChoice(id = "Q1_MCQ_1", instruction = "Choose the correct letter.", questionText = "What is the primary cause of traffic delays?", options = listOf("A. Wildlife crossings", "B. Bridge construction", "C. Seasonal flooding"), correctAnswer = "B"),
                    ListeningQuestion.MultipleChoice(id = "Q1_MCQ_2", instruction = "Choose the correct letter.", questionText = "When is the reserve café open to the public?", options = listOf("A. On weekends only", "B. Throughout the year", "C. During summer months"), correctAnswer = "A"),
                    ListeningQuestion.MapLabeling(id = "Q1_MAP_1", instruction = "Write the correct letter, A-E.", questionText = "Student Help Center Office", correctAnswer = "B"),
                    ListeningQuestion.MapLabeling(id = "Q1_MAP_2", instruction = "Write the correct letter, A-E.", questionText = "Main Lecture Hall Complex", correctAnswer = "C"),
                    ListeningQuestion.Matching(id = "Q1_MATCH_1", instruction = "Classify the historical periods.", questionText = "Gothic Arches System", categories = listOf("MEDIEVAL", "RENAISSANCE", "MODERN"), correctAnswer = "MEDIEVAL"),
                    ListeningQuestion.Matching(id = "Q1_MATCH_2", instruction = "Classify the historical periods.", questionText = "Steel Beam Foundations", categories = listOf("MEDIEVAL", "RENAISSANCE", "MODERN"), correctAnswer = "MODERN"),
                    ListeningQuestion.MultipleChoice(id = "Q1_MCQ_3", instruction = "Choose the correct letter.", questionText = "What is the new library opening hour on Sundays?", options = listOf("A. 9:00 AM", "B. 10:00 AM", "C. 12:00 PM"), correctAnswer = "B"),
                    ListeningQuestion.FormCompletion(id = "Q1_FORM_BOX", instruction = "Write ONE WORD ONLY.", questionText = "Book Return Box Location", correctAnswer = "ENTRANCE")
                )
            )
        ),

        // Script 2: Library Orientation
        MockTest(
            script = """
                Good morning, everyone, and welcome to the central library orientation. I'm the head librarian. Let's begin with the registration requirements.
                To get your permanent library card, you will need to provide a valid form of identification. While a driver's license is acceptable, the mandatory document required by the university is your PASSPORT. Please ensure you have it with you.
                When you're registering, you'll also be asked to provide a secondary contact. We require an EMAIL address rather than a phone number, as all our overdue notices are sent digitally.
                Now, about our facilities. The quiet study areas are located on the second floor. Some students assume the group study rooms are free to use anytime, but they actually require ADVANCE booking through the online portal.
                If you need to print documents, the printing station is not in the computer lab as it was last year. It has been moved to the LOBBY for easier access.
                Regarding borrowing limits, undergraduate students are allowed to take out up to 15 items. However, postgraduate students have a higher limit and can borrow up to TWENTY books at a time.
                Let's look at the library map. The Reference Section is located in area D, just past the main stairs. The Multimedia Room, where you can find audio-visual materials, is located in area E, near the café.
                For the library workshops, the 'Advanced Research Skills' session is scheduled for TUESDAY. The 'Citation Management' workshop, which many of you asked about, will be held on THURSDAY.
                Finally, if you lose your library card, there is a replacement fee. It was previously £5, but it has recently been increased to £10.
            """.trimIndent(),
            section = ListeningSection(
                sectionNumber = 2,
                environment = ListeningEnvironment.SOCIAL_MONOLOGUE,
                accent = Accent.EUROPEAN,
                audioAssetPath = "audio/section_2.mp3",
                questions = listOf(
                    ListeningQuestion.FormCompletion(id = "Q2_FORM_ID", instruction = "Write ONE WORD ONLY.", questionText = "Mandatory identification required", correctAnswer = "PASSPORT"),
                    ListeningQuestion.FormCompletion(id = "Q2_FORM_CONTACT", instruction = "Write ONE WORD ONLY.", questionText = "Secondary contact method", correctAnswer = "EMAIL"),
                    ListeningQuestion.MultipleChoice(id = "Q2_MCQ_1", instruction = "Choose the correct letter.", questionText = "How can students use group study rooms?", options = listOf("A. Free to use anytime", "B. Require advance booking", "C. Reserved for staff only"), correctAnswer = "B"),
                    ListeningQuestion.MultipleChoice(id = "Q2_MCQ_2", instruction = "Choose the correct letter.", questionText = "Where is the printing station located now?", options = listOf("A. Computer lab", "B. Second floor", "C. Lobby"), correctAnswer = "C"),
                    ListeningQuestion.FormCompletion(id = "Q2_FORM_LIMIT", instruction = "Write ONE WORD OR NUMBER.", questionText = "Borrowing limit for postgraduates", correctAnswer = "TWENTY"),
                    ListeningQuestion.MapLabeling(id = "Q2_MAP_1", instruction = "Write the correct letter, A-E.", questionText = "Reference Section", correctAnswer = "D"),
                    ListeningQuestion.MapLabeling(id = "Q2_MAP_2", instruction = "Write the correct letter, A-E.", questionText = "Multimedia Room", correctAnswer = "E"),
                    ListeningQuestion.Matching(id = "Q2_MATCH_1", instruction = "Match the workshop to the day.", questionText = "Advanced Research Skills", categories = listOf("MONDAY", "TUESDAY", "THURSDAY"), correctAnswer = "TUESDAY"),
                    ListeningQuestion.Matching(id = "Q2_MATCH_2", instruction = "Match the workshop to the day.", questionText = "Citation Management", categories = listOf("MONDAY", "TUESDAY", "THURSDAY"), correctAnswer = "THURSDAY"),
                    ListeningQuestion.MultipleChoice(id = "Q2_MCQ_3", instruction = "Choose the correct letter.", questionText = "What is the replacement fee for a lost card?", options = listOf("A. £5", "B. £10", "C. £15"), correctAnswer = "B")
                )
            )
        ),

        // Script 3: Academic Discussion
        MockTest(
            script = """
                Hello, Professor Smith. I wanted to discuss my final year project on renewable energy systems. 
                Yes, come in. Let's look at your proposal. For your primary energy source focus, you mentioned solar power, but I highly recommend you switch your focus to WIND energy, as we have better local data sets available.
                That makes sense. I'll focus on wind energy. For the initial data collection phase, should I use surveys?
                Surveys can be unreliable. I suggest you rely primarily on existing SENSORS installed at the coastal facility. They will provide accurate telemetry.
                Okay, sensors it is. Now, regarding the software simulation. I know MATLAB is standard, but considering the scale of the simulation, Python might be more efficient.
                Actually, the department has just purchased a new dedicated simulation package. You must use that specific SOFTWARE for your analysis.
                Understood. Let's review the project timeline. The literature review needs to be completed by October. What about the data analysis phase?
                The data analysis must be finished by the end of NOVEMBER so you have enough time to write the final report before the Christmas break.
                Let's look at the lab map for your workspace. Your assigned workstation is desk F, right next to the windows. The main server rack, which you'll need access to, is located at position G.
                For the project presentation formats, the interim review will be a POSTER presentation. The final defense, however, will be a formal SLIDESHOW presentation.
                Finally, regarding the project funding. The department will cover equipment costs, but any travel expenses will need to be paid out of your own POCKET.
            """.trimIndent(),
            section = ListeningSection(
                sectionNumber = 3,
                environment = ListeningEnvironment.ACADEMIC_DISCUSSION,
                accent = Accent.AUSTRALIAN,
                audioAssetPath = "audio/section_3.mp3",
                questions = listOf(
                    ListeningQuestion.FormCompletion(id = "Q3_FORM_FOCUS", instruction = "Write ONE WORD ONLY.", questionText = "Recommended primary energy source focus", correctAnswer = "WIND"),
                    ListeningQuestion.FormCompletion(id = "Q3_FORM_DATA", instruction = "Write ONE WORD ONLY.", questionText = "Primary method for initial data collection", correctAnswer = "SENSORS"),
                    ListeningQuestion.MultipleChoice(id = "Q3_MCQ_1", instruction = "Choose the correct letter.", questionText = "What tool must be used for the simulation?", options = listOf("A. MATLAB", "B. Python", "C. Dedicated department software"), correctAnswer = "C"),
                    ListeningQuestion.MultipleChoice(id = "Q3_MCQ_2", instruction = "Choose the correct letter.", questionText = "When must the data analysis be completed?", options = listOf("A. October", "B. November", "C. December"), correctAnswer = "B"),
                    ListeningQuestion.MapLabeling(id = "Q3_MAP_1", instruction = "Write the correct letter, F-J.", questionText = "Assigned workstation", correctAnswer = "F"),
                    ListeningQuestion.MapLabeling(id = "Q3_MAP_2", instruction = "Write the correct letter, F-J.", questionText = "Main server rack", correctAnswer = "G"),
                    ListeningQuestion.Matching(id = "Q3_MATCH_1", instruction = "Match the presentation format.", questionText = "Interim review", categories = listOf("POSTER", "SLIDESHOW", "REPORT"), correctAnswer = "POSTER"),
                    ListeningQuestion.Matching(id = "Q3_MATCH_2", instruction = "Match the presentation format.", questionText = "Final defense", categories = listOf("POSTER", "SLIDESHOW", "REPORT"), correctAnswer = "SLIDESHOW"),
                    ListeningQuestion.MultipleChoice(id = "Q3_MCQ_3", instruction = "Choose the correct letter.", questionText = "How will travel expenses be funded?", options = listOf("A. Department budget", "B. University grant", "C. Out of pocket"), correctAnswer = "C"),
                    ListeningQuestion.FormCompletion(id = "Q3_FORM_FUND", instruction = "Write ONE WORD ONLY.", questionText = "Department will cover the costs for", correctAnswer = "EQUIPMENT")
                )
            )
        ),

        // Script 4: Academic Lecture
        MockTest(
            script = """
                Welcome to today's lecture on advanced materials science. We will be focusing on the development of heat-resistant alloys used in aerospace engineering.
                The primary material we use for the outer heat shield is no longer aluminum, which melts at too low a temperature. Instead, we have transitioned entirely to TITANIUM due to its high strength-to-weight ratio.
                During re-entry, these shields experience extreme conditions. While the internal cabin temperature must remain comfortable, the external operational temperature can reach up to 1500 degrees Celsius.
                To mitigate this heat, we apply a specialized coating. You might think this coating is primarily for insulation, but its main function is actually REFLECTION, bouncing thermal radiation away from the hull.
                The manufacturing process has also evolved. We have abandoned traditional casting methods in favor of 3D printing, specifically using a technique called selective laser SINTERING.
                Let's look at a diagram of the spacecraft hull. The primary sensor array is located at position H, right at the nose cone. The emergency thermal vents are situated at position J, along the lateral edges.
                In terms of performance testing, the structural integrity tests are conducted in a vacuum CHAMBER. The thermal stress tests, however, utilize a high-powered plasma TUNNEL.
                A key challenge is preventing micro-fractures. We discovered that introducing trace amounts of carbon improves flexibility, but adding too much makes the alloy BRITTLE, which is catastrophic.
                Looking to the future, we are researching self-healing materials. Initial results show promise, with an expected implementation timeline of approximately FIVE years for commercial flights.
            """.trimIndent(),
            section = ListeningSection(
                sectionNumber = 4,
                environment = ListeningEnvironment.ACADEMIC_LECTURE,
                accent = Accent.STANDARD,
                audioAssetPath = "audio/section_4.mp3",
                questions = listOf(
                    ListeningQuestion.FormCompletion(id = "Q4_FORM_MAT", instruction = "Write ONE WORD ONLY.", questionText = "Primary material for outer heat shield", correctAnswer = "TITANIUM"),
                    ListeningQuestion.FormCompletion(id = "Q4_FORM_TEMP", instruction = "Write NUMBERS ONLY.", questionText = "Maximum external operational temperature", correctAnswer = "1500"),
                    ListeningQuestion.MultipleChoice(id = "Q4_MCQ_1", instruction = "Choose the correct letter.", questionText = "What is the main function of the specialized coating?", options = listOf("A. Insulation", "B. Reflection", "C. Aerodynamics"), correctAnswer = "B"),
                    ListeningQuestion.MultipleChoice(id = "Q4_MCQ_2", instruction = "Choose the correct letter.", questionText = "Which manufacturing technique is currently used?", options = listOf("A. Traditional casting", "B. Selective laser sintering", "C. Injection molding"), correctAnswer = "B"),
                    ListeningQuestion.MapLabeling(id = "Q4_MAP_1", instruction = "Write the correct letter, H-M.", questionText = "Primary sensor array", correctAnswer = "H"),
                    ListeningQuestion.MapLabeling(id = "Q4_MAP_2", instruction = "Write the correct letter, H-M.", questionText = "Emergency thermal vents", correctAnswer = "J"),
                    ListeningQuestion.Matching(id = "Q4_MATCH_1", instruction = "Match the test to the facility.", questionText = "Structural integrity tests", categories = listOf("VACUUM_CHAMBER", "PLASMA_TUNNEL", "WIND_TUNNEL"), correctAnswer = "VACUUM_CHAMBER"),
                    ListeningQuestion.Matching(id = "Q4_MATCH_2", instruction = "Match the test to the facility.", questionText = "Thermal stress tests", categories = listOf("VACUUM_CHAMBER", "PLASMA_TUNNEL", "WIND_TUNNEL"), correctAnswer = "PLASMA_TUNNEL"),
                    ListeningQuestion.FormCompletion(id = "Q4_FORM_FRAC", instruction = "Write ONE WORD ONLY.", questionText = "Excessive carbon makes the alloy", correctAnswer = "BRITTLE"),
                    ListeningQuestion.FormCompletion(id = "Q4_FORM_TIME", instruction = "Write ONE WORD ONLY.", questionText = "Expected implementation timeline in years", correctAnswer = "FIVE")
                )
            )
        ),

        // Script 5: Conference Registration
        MockTest(
            script = """
                Hello, I'm here to register for the international technology conference. I pre-registered online.
                Great, let me find your details. Your last name is Thompson, right? Yes, here it is. Your conference ID number is 88492.
                Excellent. Now, regarding the conference fee, I know the standard rate is £100, but I applied for the student discount.
                Ah yes, I see that here. With the student discount applied, your final registration fee is £60. You can pay that now.
                Okay, here is my card. Where is the opening keynote address taking place? I saw on the draft schedule it was in the Main Lobby.
                There's been a slight change. The keynote address has been moved. It will now take place in Conference Room C, as we needed a larger capacity.
                That's good to know. And what about the networking lunch? Is that still scheduled for 12:30 PM?
                Yes, the networking lunch remains at 12:30 PM, but please note that the afternoon workshops have been pushed back and will now start at exactly 2:00 PM.
                Let's look at the venue map. The exhibitor hall is located in area K, right next to the escalators. The cloakroom, where you can leave your bags, is in area L, near the main entrance.
                For the workshop tracks, the 'Artificial Intelligence' sessions are designed for ADVANCED attendees. The 'Web Development' track is intended for BEGINNERS.
                Lastly, a reminder about the evening gala. The dress code is formal, and a TICKET is required for entry, which you'll find in your welcome pack.
            """.trimIndent(),
            section = ListeningSection(
                sectionNumber = 5,
                environment = ListeningEnvironment.SOCIAL_DIALOGUE,
                accent = Accent.STANDARD,
                audioAssetPath = "audio/section_5.mp3",
                questions = listOf(
                    ListeningQuestion.FormCompletion(id = "Q5_FORM_ID", instruction = "Write NUMBERS ONLY.", questionText = "Conference ID number", correctAnswer = "88492"),
                    ListeningQuestion.FormCompletion(id = "Q5_FORM_FEE", instruction = "Write NUMBERS ONLY.", questionText = "Final registration fee (£)", correctAnswer = "60"),
                    ListeningQuestion.MultipleChoice(id = "Q5_MCQ_1", instruction = "Choose the correct letter.", questionText = "Where is the opening keynote address taking place?", options = listOf("A. Main Lobby", "B. Conference Room C", "C. Student Lounge"), correctAnswer = "B"),
                    ListeningQuestion.MultipleChoice(id = "Q5_MCQ_2", instruction = "Choose the correct letter.", questionText = "When will the afternoon workshops start?", options = listOf("A. 12:30 PM", "B. 1:30 PM", "C. 2:00 PM"), correctAnswer = "C"),
                    ListeningQuestion.MapLabeling(id = "Q5_MAP_1", instruction = "Write the correct letter, K-P.", questionText = "Exhibitor hall", correctAnswer = "K"),
                    ListeningQuestion.MapLabeling(id = "Q5_MAP_2", instruction = "Write the correct letter, K-P.", questionText = "Cloakroom", correctAnswer = "L"),
                    ListeningQuestion.Matching(id = "Q5_MATCH_1", instruction = "Match the track to the intended audience.", questionText = "Artificial Intelligence", categories = listOf("BEGINNERS", "INTERMEDIATE", "ADVANCED"), correctAnswer = "ADVANCED"),
                    ListeningQuestion.Matching(id = "Q5_MATCH_2", instruction = "Match the track to the intended audience.", questionText = "Web Development", categories = listOf("BEGINNERS", "INTERMEDIATE", "ADVANCED"), correctAnswer = "BEGINNERS"),
                    ListeningQuestion.FormCompletion(id = "Q5_FORM_GALA", instruction = "Write ONE WORD ONLY.", questionText = "Requirement for evening gala entry", correctAnswer = "TICKET"),
                    ListeningQuestion.MultipleChoice(id = "Q5_MCQ_3", instruction = "Choose the correct letter.", questionText = "What is the dress code for the evening gala?", options = listOf("A. Casual", "B. Business Casual", "C. Formal"), correctAnswer = "C")
                )
            )
        )
    )
}
