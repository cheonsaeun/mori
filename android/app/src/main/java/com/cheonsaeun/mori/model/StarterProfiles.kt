package com.cheonsaeun.mori.model

/** One-time imported profiles; actual vehicle state and seat identity are never seeded. */
object StarterProfiles {
    fun profiles() = listOf(
        PersonProfile("example-dad","아빠",SeatPreference(28,48,55,0,2),ClimatePreference(22f,3),MediaPreference(false,"재즈"),Relationship.FAMILY),
        PersonProfile("example-mom","엄마",SeatPreference(34,40,60,2,0),ClimatePreference(24f,2),MediaPreference(false,"어쿠스틱"),Relationship.FAMILY),
        PersonProfile("example-daughter","서연",SeatPreference(backrest=30,heat=1),ClimatePreference(),MediaPreference(false,"팝"),Relationship.FAMILY),
        PersonProfile("example-grandma","할머니",SeatPreference(backrest=35,heat=2),ClimatePreference(25f,1),MediaPreference(true,"클래식 라디오"),Relationship.FAMILY),
        PersonProfile("example-friend","민지",SeatPreference(backrest=32),ClimatePreference(23f,2),MediaPreference(false,"인디"),Relationship.FRIEND),
        PersonProfile("example-coworker","지훈",media=MediaPreference(false,"클래식"),relationship=Relationship.COWORKER),
    )
}

/** Preserve existing IDs and user edits during the one-time import. */
fun importStarterProfiles(existing: List<PersonProfile>): List<PersonProfile> = existing + StarterProfiles.profiles().filter {candidate -> existing.none {it.id==candidate.id}}
