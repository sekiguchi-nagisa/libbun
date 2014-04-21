#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdarg.h>

#include "karray.h"
#ifndef LIBBUN_LIB_H
#define LIBBUN_LIB_H

#define S(TEXT) (TEXT)
#define S_TOSTRING "%s"
#define S_LF       "\n"
#define S_TRUE     "true"
#define S_FALSE    "false"

static long libbun_s2i(char *x)
{
    char *end = x + strlen(x);
    return strtol(x, &end, 10);
}

static float libbun_s2f(char *x)
{
    char *end = x + strlen(x);
    return strtof(x, &end);
}

static char *libbun_f2s(float x)
{
    char buf[128];
    snprintf(buf, 128, "%f", x);
    return strndup(buf, strlen(buf));
}

static char *libbun_i2s(long x)
{
    char buf[128];
    snprintf(buf, 128, "%ld", x);
    return strndup(buf, strlen(buf));
}

static char *libbun_concat(char *left, char *right)
{
    int leftLen  = strlen(left);
    if (leftLen == 0) {
        return right;
    }
    int rightLen = strlen(right);
    if (rightLen == 0) {
        return right;
    }

    char *newstr = (char *) malloc(leftLen + rightLen + 1);
    memcpy(newstr, left, leftLen);
    memcpy(newstr + leftLen, right, rightLen);
    newstr[leftLen + rightLen] = 0;
    return newstr;
}

static void *LibZen_Malloc(size_t size)
{
  // FIXME use libgc
  return malloc(size);
}

typedef const char *const_char_ptr_t;
typedef void* void_ptr_t;
#define DEF_ARRAY_T_OP_NOPOINTER(T) DEF_ARRAY_T(T);DEF_ARRAY_OP_NOPOINTER(T)
DEF_ARRAY_STRUCT(int);
DEF_ARRAY_T_OP_NOPOINTER(int);
DEF_ARRAY_STRUCT(long);
DEF_ARRAY_T_OP_NOPOINTER(long);
DEF_ARRAY_STRUCT(double);
DEF_ARRAY_T_OP_NOPOINTER(double);
DEF_ARRAY_STRUCT(const_char_ptr_t);
DEF_ARRAY_T_OP_NOPOINTER(const_char_ptr_t);
DEF_ARRAY_STRUCT(void_ptr_t);
DEF_ARRAY_T_OP_NOPOINTER(void_ptr_t);

static ARRAY(int) *LibZen_NewBoolArray(int argc, ...)
{
  ARRAY(int) *newary = LibZen_Malloc(sizeof(*newary));
  ARRAY_init(int, newary, argc);
  int i;
  va_list list;
  va_start(list, argc);
  for (i = 0; i < argc; i++) {
    int v = va_arg(list, int);
    ARRAY_add(int, newary, v);
  }
  va_end(list);
  return newary;
}

static ARRAY(long) *LibZen_NewIntArray(int argc, ...)
{
  ARRAY(long) *newary = LibZen_Malloc(sizeof(*newary));
  ARRAY_init(long, newary, argc);
  int i;
  va_list list;
  va_start(list, argc);
  for (i = 0; i < argc; i++) {
    long v = va_arg(list, long);
    ARRAY_add(long, newary, v);
  }
  va_end(list);
  return newary;
}

static ARRAY(double) *LibZen_NewDoubleArray(int argc, ...)
{
  ARRAY(double) *newary = LibZen_Malloc(sizeof(*newary));
  ARRAY_init(double, newary, argc);
  int i;
  va_list list;
  va_start(list, argc);
  for (i = 0; i < argc; i++) {
    double v = va_arg(list, double);
    ARRAY_add(double, newary, v);
  }
  va_end(list);
  return newary;
}

static ARRAY(const_char_ptr_t) *LibZen_NewStringArray(int argc, ...)
{
  ARRAY(const_char_ptr_t) *newary = LibZen_Malloc(sizeof(*newary));
  ARRAY_init(const_char_ptr_t, newary, argc);
  int i;
  va_list list;
  va_start(list, argc);
  for (i = 0; i < argc; i++) {
    const_char_ptr_t v = va_arg(list, const_char_ptr_t);
    ARRAY_add(const_char_ptr_t, newary, v);
  }
  va_end(list);
  return newary;
}

static ARRAY(void_ptr_t) *LibZen_NewArray(int argc, ...)
{
  ARRAY(void_ptr_t) *newary = LibZen_Malloc(sizeof(*newary));
  ARRAY_init(void_ptr_t, newary, argc);
  int i;
  va_list list;
  va_start(list, argc);
  for (i = 0; i < argc; i++) {
    void_ptr_t v = va_arg(list, void_ptr_t);
    ARRAY_add(void_ptr_t, newary, v);
  }
  va_end(list);
  return newary;
}

#define libbun_ArrayLength(A) ARRAY_size((*A))

#define libbun_ArrayAdd(A, V) libbun_ArrayAdd_((ARRAY(long) *)A, (long)V)
static void libbun_ArrayAdd_(ARRAY(long) *a, long v)
{
  ARRAY_add(long, a, v);
}

#define libbun_ArraySet(A, I, V) libbun_ArraySet_((ARRAY(long) *)A, I, (long)V)
static void libbun_ArraySet_(ARRAY(long) *a, int idx, long v)
{
  ARRAY_set(long, a, idx, v);
}

#define libbun_ArrayToString(A) libbun_ArrayToString_((ARRAY(long) *)A)
static char *libbun_ArrayToString_(ARRAY(long) *a)
{
  char *tmp, *newstr;
  char *str = LibZen_Malloc(2);
  str[0] = '[';
  str[1] = 0;
  long *x, *e;
  FOR_EACH_ARRAY(*a, x, e) {
    tmp = libbun_i2s(*x);
    newstr = libbun_concat(str, tmp);
    free(tmp);
    free(str);
    str = newstr;
  }
  newstr = libbun_concat(str, "]");
  free(str);
  return newstr;
}

static int libbun_startsWith(char *self, char *prefix, size_t toffset)
{
  const char *ttext = (self) + toffset;
  return strncmp(ttext, prefix, strlen(prefix)) == 0;
}

static int libbun_endsWith(char *self, char *suffix)
{
  size_t tlen = strlen(self);
  size_t slen = strlen(suffix);
  return strncmp(self + tlen - slen, suffix, slen) == 0;
}

static char *libbun_substring(char *self, size_t beginIndex, size_t endIndex)
{
  size_t len = strlen(self);
  if(beginIndex > len) {
    // OutOfBounds
    return NULL;
  }
  if(endIndex > len || beginIndex > endIndex) {
    // OutOfBounds
    return NULL;
  }
  char *newstr = LibZen_Malloc(endIndex - beginIndex + 1);
  memcpy(newstr, self + beginIndex, endIndex - beginIndex);
  return newstr;
}

#endif /* end of include guard */
