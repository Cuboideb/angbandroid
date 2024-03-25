/* File: z-rand.h */

/*
 * Copyright (c) 1997 Ben Harrison, and others
 *
 * This software may be copied and distributed for educational, research,
 * and not for profit purposes provided that this copyright and statement
 * are included in all such copies.  Other copyrights may also apply.
 */

#ifndef INCLUDED_Z_RAND_H
#define INCLUDED_Z_RAND_H

#include "h-basic.h"



/**** Available constants ****/

/**
 * Assumed maximum dungeon level.  This value is used for various
 * calculations involving object and monster creation.  It must be at least
 * 100. Setting it below 128 may prevent the creation of some objects.
 */
#define MAX_RAND_DEPTH	128

/**
 * A struct representing a strategy for making a dice roll.
 *
 * The result will be base + XdY + BONUS, where m_bonus is used in a
 * tricky way to determine BONUS.
 */
typedef struct random {
	int base;
	int dice;
	int sides;
	int m_bonus;
} random_value;

/**
 * A struct representing a random chance of success, such as 8 in 125 (6.4%).
 */
typedef struct random_chance_s {
	int32_t numerator;
	int32_t denominator;
} random_chance;

/**
 * The number of 32-bit integers worth of seed state.
 */
#define OLD_RAND_DEG 63
#define RAND_DEG 32

/**
 * Random aspects used by damcalc, m_bonus_calc, and ranvals
 */
typedef enum {
	MINIMISE,
	AVERAGE,
	MAXIMISE,
	EXTREMIFY,
	RANDOMISE
} aspect;


/**** Available macros ****/


/*
 * Generates a random long integer X where O<=X<M.
 * The integer X falls along a uniform distribution.
 * For example, if M is 100, you get "percentile dice"
 */
#define rand_int(M) \
	((s32b)(Rand_div(M)))


/*
 * Generates a random long integer X where 1<=X<=M.
 *
 * Note that the behaviour for M < 1 is undefined.
 */
#define randint(M) \
	(rand_int(M) + 1)


/*
 * Generates a random long integer X where O<=X<M.
 * The integer X falls along a uniform distribution.
 * For example, if M is 100, you get "percentile dice"
 *
 * The same as rand_int().
 */
#define randint0(M) \
	((s32b)Rand_div(M))


/*
 * Generates a random long integer X where 1<=X<=M.
 *
 * Also, "correctly" handle the case of M<=1
 *
 * The same as randint().
 */
#define randint1(M) \
	(randint0(M) + 1)


/*
 * Generates a random long integer X where A<=X<=B
 * The integer X falls along a uniform distribution.
 * Note: rand_range(0,N-1) == randint0(N)
 */
/*
#define rand_range(A,B) \
	((A) + (randint0(1+(B)-(A))))
*/

extern int rand_range(int A, int B);

/*
 * Generate a random long integer X where A-D<=X<=A+D
 * The integer X falls along a uniform distribution.
 * Note: rand_spread(A,D) == rand_range(A-D,A+D)
 */
#define rand_spread(A,D) \
	((A) + (randint0(1+(D)+(D))) - (D))




#define one_in_(X) \
	(randint0(X) == 0)


/*
 * Evaluate to TRUE "S" percent of the time
 */
#define saving_throw(S) \
	(randint0(100) < (S))


/**** Available Variables ****/


extern bool Rand_quick;
extern u32b Rand_value;
/*extern u16b Rand_place;*/
extern u16b state_i;
extern u32b STATE[OLD_RAND_DEG];
extern byte quick_rand_place;


/**** Available Functions ****/


extern void Rand_state_init(u32b seed);
extern s32b Rand_div(u32b m);
extern s16b Rand_normal(int mean, int stand);
extern u32b Rand_simple(u32b m);
extern s16b damroll(int num, int sides);
extern s16b maxroll(int num, int sides);
extern bool quick_rand(void);
extern void quick_rand_add(void);

#endif /* INCLUDED_Z_RAND_H */
