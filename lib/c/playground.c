// @TODO
#define TODO(x)    x

// @s2i;string.h;stdlib.h
static long libbun_s2i(const char *x)
{
    char *end = x + strlen(x);
    return strtol(x, &end, 10);
}

// @s2f;string.h;stdlib.h
static double libbun_s2f(const char *x)
{
    char *end = x + strlen(x);
    return strtod(x, &end);
}

// @f2s;string.h;stdlib.h;stdio.h
static const char *libbun_f2s(float x)
{
    char buf[128];
    snprintf(buf, 128, "%f", x);
    return (const char *)strndup(buf, strlen(buf));
}

// @i2s;string.h;stdlib.h;stdio.h
static const char *libbun_i2s(long x)
{
    char buf[128];
    snprintf(buf, 128, "%ld", x);
    return (const char *)strndup(buf, strlen(buf));
}

// @concat;string.h;stdlib.h
static const char *libbun_concat(const char *left, const char *right)
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

// @startsWith;string.h;stdlib.h
static int libbun_startsWith(const char *self, const char *prefix, size_t toffset)
{
  const char *ttext = (self) + toffset;
  return strncmp(ttext, prefix, strlen(prefix)) == 0;
}

// @endsWith;string.h;stdlib.h
static int libbun_endsWith(const char *self, const char *suffix)
{
  size_t tlen = strlen(self);
  size_t slen = strlen(suffix);
  return strncmp(self + tlen - slen, suffix, slen) == 0;
}

static const char *libbun_substring(const char *self, size_t beginIndex, size_t endIndex)
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
  return (const char *)newstr;
}

